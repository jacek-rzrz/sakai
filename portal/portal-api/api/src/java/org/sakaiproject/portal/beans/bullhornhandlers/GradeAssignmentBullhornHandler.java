/**
 * Copyright (c) 2003-2017 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.portal.beans.bullhornhandlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.sakaiproject.assignment.api.AssignmentConstants;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.model.Assignment;
import org.sakaiproject.assignment.api.model.AssignmentSubmission;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.portal.api.BullhornData;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GradeAssignmentBullhornHandler extends AbstractBullhornHandler {

    @Inject
    private AssignmentService assignmentService;

    @Override
    public List<String> getHandledEvents() {
        return Arrays.asList(AssignmentConstants.EVENT_GRADE_ASSIGNMENT_SUBMISSION);
    }

    @Override
    public Optional<List<BullhornData>> handleEvent(Event e, Cache<String, Map> countCache) {

        String from = e.getUserId();

        String ref = e.getResource();
        String[] pathParts = ref.split("/");

        String siteId = pathParts[3];
        String submissionId = pathParts[pathParts.length - 1];
        try {
            AssignmentSubmission submission = assignmentService.getSubmission(submissionId);
            if (submission.getGradeReleased()) {
                Assignment assignment = submission.getAssignment();
                String title = assignment.getTitle();
                List<BullhornData> bhEvents = new ArrayList<>();
                submission.getSubmitters().forEach(to -> {
                    try {
                        bhEvents.add(new BullhornData(from, to.getSubmitter(), siteId, title, assignmentService.getDeepLink(siteId, assignment.getId(), to.getSubmitter()), false));
                        countCache.remove(to.getSubmitter());
                    } catch(Exception exc) {
                        log.error("Error retrieving deep link for assignment {} and user {} on site {}", assignment.getId(), to.getSubmitter(), siteId, exc);
                    }
                });

                return Optional.of(bhEvents);
            }
        } catch (Exception ex) {
            log.error("Failed to find either the submission or the site", ex);
        }

        return Optional.empty();
    }
}

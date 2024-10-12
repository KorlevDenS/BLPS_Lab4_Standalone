package org.korolev.dens.blps_lab4_standalone.camunda;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.korolev.dens.blps_lab4_standalone.aspects.Authorize;
import org.korolev.dens.blps_lab4_standalone.aspects.ConfusionReport;
import org.korolev.dens.blps_lab4_standalone.entites.Chapter;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForbiddenActionException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumLogicException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumObjectNotFoundException;
import org.korolev.dens.blps_lab4_standalone.services.ChapterService;
import org.korolev.dens.blps_lab4_standalone.services.TopicService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ChapterTaskList {

    private final ChapterService chapterService;
    private final TopicService topicService;

    public ChapterTaskList(ChapterService chapterService, TopicService topicService) {
        this.chapterService = chapterService;
        this.topicService = topicService;
    }

    @Authorize
    @ConfusionReport
    public void performAddChapter(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForbiddenActionException {
        String addedTitle = externalTask.getVariable("addedTitle");
        String addedDescription = externalTask.getVariable("addedDescription");
        Chapter chapter = new Chapter();
        chapter.setTitle(addedTitle);
        chapter.setDescription(addedDescription);
        chapter.setCreated(LocalDate.now());
        Chapter added = chapterService.add(chapter);
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("addedChapter", added);
        externalTaskService.complete(externalTask, variableMap);
    }

    public void performGetAllChapters(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        List<Chapter> chapters = chapterService.findAllChapters();
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("chapters", chapters);
        externalTaskService.complete(externalTask, variableMap);
    }

    @ConfusionReport
    public void performCheckForTopics(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumObjectNotFoundException {
        Integer chapterId = externalTask.getVariable("selected");
        boolean empty = topicService.findAllByChapter(chapterId).isEmpty();
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("isEmpty", empty);
        externalTaskService.complete(externalTask, variableMap);
    }

    @Authorize
    @ConfusionReport
    public void performDelete(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumLogicException, ForumObjectNotFoundException {
        Integer chapterId = externalTask.getVariable("selected");
        chapterService.delete(chapterId);
        externalTaskService.complete(externalTask);
    }

}

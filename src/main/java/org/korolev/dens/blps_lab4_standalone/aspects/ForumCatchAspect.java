package org.korolev.dens.blps_lab4_standalone.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(1)
@Component
public class ForumCatchAspect {

    @Around(
            value = "@annotation(ConfusionReport) && args(externalTask, externalTaskService)",
            argNames = "joinPoint,externalTask,externalTaskService"
    )
    public Object handleForumExceptions(ProceedingJoinPoint joinPoint, ExternalTask externalTask,
                                        ExternalTaskService externalTaskService) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (ForumException e) {
            externalTaskService.handleBpmnError(externalTask, e.getPossibleStatus().toString(), e.getMessage());
            return null;
        }
    }

}

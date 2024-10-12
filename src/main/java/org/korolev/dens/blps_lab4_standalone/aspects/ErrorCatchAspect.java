package org.korolev.dens.blps_lab4_standalone.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Aspect
@Order(0)
@Component
public class ErrorCatchAspect {

    @Around(
            value = "@annotation(ConfusionReport) && args(externalTask, externalTaskService)",
            argNames = "joinPoint,externalTask,externalTaskService"
    )
    public Object handleExceptions(ProceedingJoinPoint joinPoint, ExternalTask externalTask,
                                   ExternalTaskService externalTaskService) throws Throwable {
        try {
            joinPoint.proceed();
            return null;
        } catch (Exception e) {
            externalTaskService.handleBpmnError(externalTask, HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    "Not handled server error");
            System.out.println("NOT HANDLED EXCEPTION: " + e.getMessage());
            return null;
        }
    }

}

package org.korolev.dens.blps_lab4_standalone.aspects;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.korolev.dens.blps_lab4_standalone.services.ClientService;
import org.korolev.dens.blps_lab4_standalone.services.JwtTokenService;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Aspect
@Order(2)
@Component
public class JwtAuthAspect {

    private final JwtTokenService jwtTokenService;
    private final ClientService clientService;

    public JwtAuthAspect(JwtTokenService jwtTokenService, ClientService clientService) {
        this.jwtTokenService = jwtTokenService;
        this.clientService = clientService;
    }

    @Around(
            value = "@annotation(Authorize) && args(externalTask, externalTaskService)",
            argNames = "joinPoint,externalTask,externalTaskService"
    )
    public Object checkJwtAuthentication(ProceedingJoinPoint joinPoint, ExternalTask externalTask,
                                         ExternalTaskService externalTaskService) throws Throwable {
        String jwtToken = externalTask.getVariable("jwtToken");
        if (jwtToken == null) {
            externalTaskService.handleBpmnError(externalTask, HttpStatus.FORBIDDEN.toString(), "No jwt detected");
            return null;
        }
        try {
            String username = jwtTokenService.extractUserName(jwtToken);
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = clientService.loadUserByUsername(username);
                if (jwtTokenService.verifyToken(jwtToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    externalTaskService.handleBpmnError(externalTask, HttpStatus.FORBIDDEN.toString(), "Unverified jwt");
                    return null;
                }
            }
        } catch (SignatureException e) {
            externalTaskService.handleBpmnError(externalTask, HttpStatus.FORBIDDEN.toString(), "Invalid jwt token");
            return null;
        } catch (ExpiredJwtException e) {
            externalTaskService.handleBpmnError(externalTask, HttpStatus.FORBIDDEN.toString(), "Expired jwt token");
            return null;
        } catch (Exception e) {
            externalTaskService.handleBpmnError(externalTask, HttpStatus.FORBIDDEN.toString(), "Wrong jwt token");
            return null;
        }
        try {
            return joinPoint.proceed();
        } catch (AccessDeniedException e) {
            externalTaskService.handleBpmnError(externalTask, HttpStatus.FORBIDDEN.toString(), e.getMessage());
            return null;
        }
    }

}

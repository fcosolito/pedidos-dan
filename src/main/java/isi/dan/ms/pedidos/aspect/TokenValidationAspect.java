package isi.dan.ms.pedidos.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.Claims;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

@Aspect
@Component
public class TokenValidationAspect {

   @Autowired
   private JwtUtility jwtUtility;

   private static final Logger log = LoggerFactory.getLogger(TokenValidationAspect.class);

   @Before("@annotation(tokenValidation)")
   public void validateToken(JoinPoint joinPoint, TokenValidation tokenValidation) throws Throwable {
      String token = getTokenFromRequest();
      if (token == null) {
         throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing JWT token");
      }
      log.info("Validando token: {}", token);

      try {
         Claims claims = jwtUtility.validateToken(token);
         if (claims == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token --> TokenValidationAspect");
         }

         log.info("Token validado correctamente. Claims: {}", claims);
      } catch (Exception e) {
         log.error("Token inválido", e);
         throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token --> TokenValidationAspect");
      }
   }

   private String getTokenFromRequest() {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes == null) {
         log.warn("No se pudo obtener las attributes de la request");
         return null;
      }
      HttpServletRequest request = attributes.getRequest();
      String authHeader = request.getHeader("Authorization");
      log.info("Authorization header: {}", authHeader);
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
         return authHeader.substring(7);
      }
      log.warn("No se encontró el header Authorization o no tiene el formato esperado");
      return null;
   }
}
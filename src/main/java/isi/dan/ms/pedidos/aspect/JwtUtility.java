package isi.dan.ms.pedidos.aspect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

import java.nio.charset.StandardCharsets;

@Component
public class JwtUtility {

   @Value("${security.jwt.secret}")
   private String jwtSecretKey;

   private static final Logger log = LoggerFactory.getLogger(JwtUtility.class);

   public Claims validateToken(String token) {
      try {
         log.info("Clave secreta utilizada para HMAC256: {}", jwtSecretKey);

         Jws<Claims> claimsJws = Jwts.parser()
               .setSigningKey(jwtSecretKey.getBytes(StandardCharsets.UTF_8))
               .parseClaimsJws(token);

         Claims claims = claimsJws.getBody();
         log.info("Token JWT decodificado correctamente: {}", claims);

         // Puedes establecer valores adicionales en algún contexto de seguridad si es
         // necesario
         return claims;
      } catch (SignatureException e) {
         log.error("Firma del token inválida: {}", e.getMessage());
         throw new SignatureException("Token inválido");
      } catch (ExpiredJwtException e) {
         log.error("Token expirado: {}", e.getMessage());
         throw new RuntimeException("Token expirado");
      } catch (UnsupportedJwtException e) {
         log.error("Token JWT no soportado: {}", e.getMessage());
         throw new RuntimeException("Token no soportado");
      } catch (IllegalArgumentException e) {
         log.error("El token JWT es nulo o está vacío: {}", e.getMessage());
         throw new RuntimeException("Token inválido");
      } catch (Exception e) {
         log.error("Error al validar el token: {}", e.getMessage());
         throw new RuntimeException("Error al validar el token", e);
      }
   }
}

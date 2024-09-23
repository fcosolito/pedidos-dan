package isi.dan.ms.pedidos.feignClients;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

   @Override
   public void apply(RequestTemplate requestTemplate) {
      ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
            .getRequestAttributes();
      if (requestAttributes != null) {
         String token = requestAttributes.getRequest().getHeader("Authorization");
         if (token != null) {
            requestTemplate.header("Authorization", token);
         }
      }
   }
}

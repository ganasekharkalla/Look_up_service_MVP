package com.twilio.lookup.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Maps the friendly page paths to the static HTML files. The API stays under {@code /api/v1}. */
@Configuration(proxyBeanMethods = false)
public class WebPageConfig implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/phone").setViewName("forward:/phone.html");
		registry.addViewController("/email").setViewName("forward:/email.html");
	}
}

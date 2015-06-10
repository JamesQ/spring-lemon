package com.naturalprogrammer.spring.boot.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import com.naturalprogrammer.spring.boot.SaUtil;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	private static final String REMEMBER_ME_COOKIE = "rememberMe";
	private static final String REMEMBER_ME_PARAMETER = "rememberMe";
	//public static final String CSRF_TOKEN_HEADER = "X-XSRF-TOKEN";
	
	@Value(SaUtil.APPLICATION_URL)
	private String applicationUrl;
	
	@Value("${rememberMe.privateKey}")
	private String rememberMeKey;
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	@Autowired
	private AuthSuccess authSuccess;
	
	@Autowired
	private LogoutSuccessHandler logoutSuccessHandler;
	
    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
    	return new SimpleUrlAuthenticationFailureHandler();
    }
    
	@Bean
    public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
    
    @Bean
    public RememberMeServices rememberMeServices() {
    	
        TokenBasedRememberMeServices rememberMeServices = new TokenBasedRememberMeServices(rememberMeKey, userDetailsService);
        rememberMeServices.setParameter(REMEMBER_ME_PARAMETER); // default is "remember-me" (in earlier spring security versions it was "_spring_security_remember_me")
        rememberMeServices.setCookieName(REMEMBER_ME_COOKIE);
        return rememberMeServices;
        
    }
    
	@Bean
	public SwitchUserFilter switchUserFilter() {
		SwitchUserFilter filter = new SwitchUserFilter();
		filter.setUserDetailsService(userDetailsService);
		filter.setSuccessHandler(authSuccess);
		filter.setFailureHandler(authenticationFailureHandler());
		//filter.setSwitchUserUrl("/j_spring_security_switch_user");
		//filter.setExitUserUrl("/j_spring_security_exit_user");
		//filter.setTargetUrl(applicationUrl);
		return filter;
	}
	
    @Autowired
    @Override
    protected void configure(AuthenticationManagerBuilder authManagerBuilder) throws Exception {
        authManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		
		http
			.formLogin()
//				.loginPage("/login")
//				.permitAll()
				
				/******************************************
				 * Setting a successUrl would redirect the user there. Instead,
				 * let's send 200 and the userDto.
				 *****************************************/
				.successHandler(authSuccess)
				
				/*******************************************
				 * Setting the failureUrl will redirect the user to
				 * that url if login fails. Instead, we need to send
				 * 401. So, let's set failureHandler instead.
				 * 
				 * Debug org.apache.catalina.core.StandardHostValve's
				 * private void status(Request request, Response response)
				 * if you want to understand why we get in the log. It's not a problem, as I understand.
				 * 2015-05-06 13:56:26.908 DEBUG 10184 --- [nio-8080-exec-1] o.s.b.a.e.mvc.EndpointHandlerMapping     : Did not find handler method for [/error]
				 * 2015-05-06 13:56:45.007 DEBUG 10184 --- [nio-8080-exec-3] o.s.b.a.e.mvc.EndpointHandlerMapping     : Looking up handler method for path /error2
				 *******************************************/
	        	.failureHandler(authenticationFailureHandler())
	        	.and()
			.logout()
			
				/************************************************
				 * To prevent redirection to home page, we need to
				 * have this custom logoutSuccessHandler
				 ***********************************************/
				.logoutSuccessHandler(logoutSuccessHandler)
				//.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID", REMEMBER_ME_COOKIE)
				.and()
			.rememberMe()
				.key(rememberMeKey)
				.rememberMeServices(rememberMeServices())
				.and()
			//.csrf().disable()
			//.csrf()
				//.csrfTokenRepository(csrfTokenRepository()).and()
			.addFilterAfter(new CsrfHeaderFilter(), CsrfFilter.class)
			.addFilterAfter(switchUserFilter(), FilterSecurityInterceptor.class)
			.authorizeRequests()
				.antMatchers("/login/impersonate*").hasRole("ADMIN")
				.antMatchers("/logout/impersonate*").authenticated()
				.antMatchers("/only-for-admin*").hasRole("ADMIN")
				//.antMatchers("/secure").authenticated()
				.antMatchers("/**").permitAll();                  
	}
	
	
//	public 
//	
//	private CsrfTokenRepository csrfTokenRepository() {
//		
//		HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
//		repository.setHeaderName(CSRF_TOKEN_HEADER);
//		return repository;
//		  
//	}
//
}

package com.meetup.liquibase;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;

@Configuration
public class LiquibaseConfiguration  implements EnvironmentAware {

  private static final Logger LOG = LoggerFactory.getLogger(LiquibaseConfiguration.class);

  private RelaxedPropertyResolver dataSourcePropertyResolver;

  private Environment env;

  private RelaxedPropertyResolver liquibasePropertyResolver;

  @Override
  public void setEnvironment(Environment env) {
    this.env = env;
    this.dataSourcePropertyResolver = new RelaxedPropertyResolver(env, "spring.datasource.");
    this.liquibasePropertyResolver = new RelaxedPropertyResolver(env, "liquibase.");
  }

  @Bean(destroyMethod = "close")
  public DataSource dataSource() {

    if (dataSourcePropertyResolver.getProperty("url") == null && dataSourcePropertyResolver.getProperty("databaseName") == null) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Your database connection pool configuration is incorrect! The application cannot start. Please check your Spring profile, current profiles are: {}", Arrays.toString(env.getActiveProfiles()));
      }
      throw new ApplicationContextException("Database connection pool is not configured correctly");
    }
    HikariConfig config = new HikariConfig();
    config.setDataSourceClassName(dataSourcePropertyResolver.getProperty("dataSourceClassName"));
    if (StringUtils.isEmpty(dataSourcePropertyResolver.getProperty("url"))) {
      config.addDataSourceProperty("databaseName", dataSourcePropertyResolver.getProperty("databaseName"));
      config.addDataSourceProperty("serverName", dataSourcePropertyResolver.getProperty("serverName"));
    } else {
      config.addDataSourceProperty("url", dataSourcePropertyResolver.getProperty("url"));
    }
    config.addDataSourceProperty("user", dataSourcePropertyResolver.getProperty("username"));
    config.addDataSourceProperty("password", dataSourcePropertyResolver.getProperty("password"));

    config.setConnectionTimeout(30000);
    config.setIdleTimeout(600000);
    config.setMaxLifetime(1800000);
    config.setMaximumPoolSize(100);
    config.setMinimumIdle(10);

    return new HikariDataSource(config);
  }

  @Bean
  public SpringLiquibase liquibase() {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource());
    liquibase.setChangeLog(liquibasePropertyResolver.getProperty("change-log"));
    liquibase.setContexts(liquibasePropertyResolver.getProperty("context"));
    liquibase.setChangeLogParameters(liquibasePropertyResolver
                                             .getSubProperties("parameters.").entrySet().stream()
                                             .collect(Collectors.toMap(Map.Entry::getKey, entry -> (String) entry.getValue())));
    return liquibase;
  }


}

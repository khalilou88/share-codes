package com.example.demo.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class SpringBootJdbcAppender extends AppenderSkeleton implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private static DataSource dataSource;
    private String sql;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringBootJdbcAppender.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        if (applicationContext != null) {
            SpringBootJdbcAppender.dataSource = applicationContext.getBean(DataSource.class);
        }
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    @Override
    protected void append(LoggingEvent event) {
        if (dataSource == null) {
            LogLog.error("DataSource is not configured for JdbcAppender");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String formattedSQL = formatSQL(event);
            try (PreparedStatement ps = conn.prepareStatement(formattedSQL)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LogLog.error("Error executing SQL for logging", e);
        }
    }

    private String formatSQL(LoggingEvent event) {
        String result = sql.replace("'%d{yyyy-MM-dd HH:mm:ss}'", "'" + dateFormat.format(new Date(event.timeStamp)) + "'");
        result = result.replace("'%p'", "'" + event.getLevel().toString() + "'");
        result = result.replace("'%c'", "'" + event.getLoggerName() + "'");

        // Escape single quotes in the message to avoid SQL injection
        String message = event.getRenderedMessage();
        if (message != null) {
            message = message.replace("'", "''");
        } else {
            message = "";
        }
        result = result.replace("'%m'", "'" + message + "'");

        // Handle the exception if present
        String throwable = "";
        if (event.getThrowableStrRep() != null) {
            StringBuilder sb = new StringBuilder();
            for (String line : event.getThrowableStrRep()) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
            throwable = sb.toString().replace("'", "''");
        }
        result = result.replace("'%throwable{1000}'", "'" + throwable + "'");

        // Thread name
        String threadName = event.getThreadName().replace("'", "''");
        result = result.replace("'%t'", "'" + threadName + "'");

        return result;
    }

    @Override
    public void close() {
        // Nothing to close
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
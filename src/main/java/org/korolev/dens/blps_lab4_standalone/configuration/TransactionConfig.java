package org.korolev.dens.blps_lab4_standalone.configuration;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import jakarta.transaction.SystemException;
import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String userName;

    @Value("${spring.datasource.password}")
    private String password;

    // encapsulate data source and provides distributed transactions
    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosDataSourceBean myDataSource() {
        PGXADataSource pgxaDataSource = new PGXADataSource();
        pgxaDataSource.setURL(url);
        pgxaDataSource.setUser(userName);
        pgxaDataSource.setPassword(password);
        AtomikosDataSourceBean atomikosdataSourceBean = new AtomikosDataSourceBean();
        atomikosdataSourceBean.setXaDataSource(pgxaDataSource);
        atomikosdataSourceBean.setUniqueResourceName("database");
        atomikosdataSourceBean.setMaxPoolSize(10);
        return atomikosdataSourceBean;
    }

    // provides methods to manage transactions in app code (commit, rollback)
    @Bean
    public UserTransactionImp userTransactionImp() {
        return new UserTransactionImp();
    }

    // manages transactions`s lifecycle
    @Bean(initMethod = "init", destroyMethod = "close")
    public UserTransactionManager userTransactionManager() throws SystemException {
        UserTransactionManager userTransactionManager = new UserTransactionManager();
        userTransactionManager.setTransactionTimeout(300);
        return userTransactionManager;
    }

    // integration between Spring and JTA
    @Bean
    public JtaTransactionManager transactionManager() throws SystemException {
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
        jtaTransactionManager.setTransactionManager(userTransactionManager());
        jtaTransactionManager.setUserTransaction(userTransactionImp());
        jtaTransactionManager.setAllowCustomIsolationLevels(true);
        return jtaTransactionManager;
    }

    // Spring interface for transactions management
    @Bean
    public PlatformTransactionManager platformTransactionManager() throws SystemException {
        return transactionManager();
    }

}

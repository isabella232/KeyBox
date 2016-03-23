/**
 * Copyright 2013 Sean Kavanagh - sean.p.kavanagh6@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.keybox.manage.util;

import com.keybox.common.util.AppConfig;
import org.apache.commons.dbcp.*;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to create a pooling data source object using commons DBCP
 *
 */
public class DSPool {

    private static Logger log = LoggerFactory.getLogger(DSPool.class);

    //system path to the H2 DB
    private static String DB_PATH = DBUtils.class.getClassLoader().getResource("keydb").getPath();


    private static PoolingDataSource dsPool;


    /**
     * fetches the data source for H2 db
     *
     * @return data source pool
     */

    public static org.apache.commons.dbcp.PoolingDataSource getDataSource() {
        if (dsPool == null) {

            dsPool = registerDataSource();
        }
        return dsPool;

    }

    /**
     * register the data source for H2 DB
     *
     * @return pooling database object
     */

    private static PoolingDataSource registerDataSource() {

        final boolean externalDbEnabled = StringUtils.isNotEmpty(AppConfig.getProperty("dbListenPort"));
        final String DB_EXTERNAL_PORT = AppConfig.getProperty("dbListenPort");
        final boolean mysqlDbEnabled = StringUtils.isNotEmpty(AppConfig.getProperty("mysqlHost"));
        String connectionURI = null;
        String validationQuery = "select 1";
    
        String user = "keybox";
        String password = "filepwd 45WJLnwhpA47EepT162hrVnDn3vYRvJhpZi0sVdvN9Sdsf";

        if(mysqlDbEnabled) {
            connectionURI = "jdbc:mysql://" + AppConfig.getProperty("mysqlHost") + ":" + AppConfig.getProperty("mysqlPort") + "/" + AppConfig.getProperty("mysqlDb") ;
            user = AppConfig.getProperty("mysqlUser");
            password = AppConfig.getProperty("mysqlPass");

        } else {

            // create a database connection

            if(externalDbEnabled) {
                connectionURI = "jdbc:h2:" + DB_PATH + "/keybox;CIPHER=AES;AUTO_SERVER=TRUE;AUTO_SERVER_PORT=" + DB_EXTERNAL_PORT;
            } else {
                connectionURI = "jdbc:h2:" + DB_PATH + "/keybox;CIPHER=AES";
            }

            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException ex) {
                log.error(ex.toString(), ex);
            }
        }

        GenericObjectPool connectionPool = new GenericObjectPool(null);

        connectionPool.setMaxActive(25);
        connectionPool.setTestOnBorrow(true);
        connectionPool.setMinIdle(2);
        connectionPool.setMaxWait(15000);
        connectionPool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);


        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectionURI, user, password);


        new PoolableConnectionFactory(connectionFactory, connectionPool, null, validationQuery, false, true);

        return new PoolingDataSource(connectionPool);

    }


}


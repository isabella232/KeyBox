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
package com.keybox.common.db;

import com.keybox.common.util.AppConfig;
import com.keybox.manage.model.Auth;
import com.keybox.manage.model.SessionOutput;
import com.keybox.manage.util.DBUtils;
import com.keybox.manage.util.EncryptionUtil;
import com.keybox.manage.util.RefreshAuthKeyUtil;
import com.keybox.manage.util.SSHUtil;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initial startup task.  Creates an SQLite DB and generates
 * the system public/private key pair if none exists
 */
@WebServlet(name = "DBInitServlet",
		urlPatterns = {"/config"},
		loadOnStartup = 1)
public class DBInitServlet extends javax.servlet.http.HttpServlet {

    private static Logger log = LoggerFactory.getLogger(DBInitServlet.class);

	/**
	 * task init method that created DB and generated public/private keys
	 *
	 * @param config task config
	 * @throws ServletException
	 */
	public void init(ServletConfig config) throws ServletException {

		super.init(config);

        String DB_EXT_USER = null;
        String DB_EXT_PASS = null;
        String dbSchema = "PUBLIC";

		Connection connection = null;
		Statement statement = null;
		//check if reset ssh application key is set
		boolean resetSSHKey = "true".equals(AppConfig.getProperty("resetApplicationSSHKey"));
        boolean dbUserEnabled = (StringUtils.isNotEmpty(AppConfig.getProperty("dbUserPass")) && StringUtils.isNotEmpty(AppConfig.getProperty("dbUserLogin")));
        if(dbUserEnabled) {
            DB_EXT_USER = AppConfig.getProperty("dbUserLogin").toUpperCase();
            DB_EXT_PASS = AppConfig.getProperty("dbUserPass");
        }
        if (StringUtils.isNotEmpty(AppConfig.getProperty("mysqlDb"))) {
            dbSchema = AppConfig.getProperty("mysqlDb");
        }
        
		try {
			connection = DBUtils.getConn();
			statement = connection.createStatement();

			ResultSet rs = statement.executeQuery("select * from information_schema.tables where upper(table_name) = 'USERS' and table_schema='" + dbSchema + "'");
			if (rs == null || !rs.next()) {
				resetSSHKey = true;
				statement.executeUpdate("create table if not exists users (id INTEGER PRIMARY KEY AUTO_INCREMENT, first_nm varchar(200), last_nm varchar(200), email varchar(200), username varchar(200) not null, password varchar(200), auth_token varchar(800), enabled boolean not null default true, auth_type varchar(50) not null default '" + Auth.AUTH_BASIC+ "', user_type varchar(50) not null default '" + Auth.ADMINISTRATOR + "', salt varchar(800), otp_secret varchar(200))");
				
				statement.executeUpdate("create table if not exists user_theme (user_id INTEGER PRIMARY KEY, bg varchar(7), fg varchar(7), d1 varchar(7), d2 varchar(7), d3 varchar(7), d4 varchar(7), d5 varchar(7), d6 varchar(7), d7 varchar(7), d8 varchar(7), b1 varchar(7), b2 varchar(7), b3 varchar(7), b4 varchar(7), b5 varchar(7), b6 varchar(7), b7 varchar(7), b8 varchar(7), foreign key (user_id) references users(id) on delete cascade) ");

				statement.executeUpdate("create table if not exists system (id INTEGER PRIMARY KEY AUTO_INCREMENT, display_nm varchar(200) not null, user varchar(200) not null, host varchar(200) not null, port INTEGER not null, authorized_keys varchar(50000) not null, status_cd varchar(200) not null default 'INITIAL')");
				statement.executeUpdate("create table if not exists profiles (id INTEGER PRIMARY KEY AUTO_INCREMENT, nm varchar(200) not null, descr varchar(200) not null)");
				statement.executeUpdate("create table if not exists system_map (profile_id INTEGER, system_id INTEGER, foreign key (profile_id) references profiles(id) on delete cascade , foreign key (system_id) references system(id) on delete cascade, primary key (profile_id, system_id))");
				statement.executeUpdate("create table if not exists user_map (user_id INTEGER, profile_id INTEGER, foreign key (user_id) references users(id) on delete cascade, foreign key (profile_id) references profiles(id) on delete cascade, primary key (user_id, profile_id))");
				statement.executeUpdate("create table if not exists application_key (id INTEGER PRIMARY KEY AUTO_INCREMENT, public_key varchar(20000) not null, private_key varchar(20000) not null, passphrase varchar(10000))");

				statement.executeUpdate("create table if not exists status (id INTEGER, user_id INTEGER, status_cd varchar(200) not null default 'INITIAL', foreign key (id) references system(id) on delete cascade, foreign key (user_id) references users(id) on delete cascade, primary key(id, user_id))");
				statement.executeUpdate("create table if not exists scripts (id INTEGER PRIMARY KEY AUTO_INCREMENT, user_id INTEGER, display_nm varchar(200) not null, script varchar(60000) not null, foreign key (user_id) references users(id) on delete cascade)");


				statement.executeUpdate("create table if not exists public_keys (id INTEGER PRIMARY KEY AUTO_INCREMENT, key_nm varchar(200) not null, type varchar(200), fingerprint varchar(10000), public_key varchar(10000), enabled boolean not null default true, create_dt timestamp not null default CURRENT_TIMESTAMP(),  user_id INTEGER, profile_id INTEGER, foreign key (profile_id) references profiles(id) on delete cascade, foreign key (user_id) references users(id) on delete cascade)");

				statement.executeUpdate("create table if not exists session_log (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id INTEGER, session_tm timestamp default CURRENT_TIMESTAMP, foreign key (user_id) references users(id) on delete cascade )");
				statement.executeUpdate("create table if not exists terminal_log (session_id BIGINT, instance_id INTEGER, system_id INTEGER, output varchar(60000) not null, log_tm timestamp default CURRENT_TIMESTAMP, foreign key (session_id) references session_log(id) on delete cascade, foreign key (system_id) references system(id) on delete cascade)");

                //set up user for external connections to db
                if(dbUserEnabled) {
				    statement.executeUpdate("CREATE USER IF NOT EXISTS " + DB_EXT_USER + " PASSWORD '" + DB_EXT_PASS + "' ADMIN");
                }

				//insert default admin user
				String salt = EncryptionUtil.generateSalt();
				PreparedStatement pStmt = connection.prepareStatement("insert into users (username, password, user_type, salt) values(?,?,?,?)");
				pStmt.setString(1, "admin");
				pStmt.setString(2, EncryptionUtil.hash("changeme" + salt));
				pStmt.setString(3, Auth.MANAGER);
				pStmt.setString(4, salt);
				pStmt.execute();
				DBUtils.closeStmt(pStmt);

			}
			DBUtils.closeRs(rs);

			//if reset ssh application key then generate new key
			if (resetSSHKey) {

				//delete old key entry
				PreparedStatement pStmt = connection.prepareStatement("delete from application_key");
				pStmt.execute();
				DBUtils.closeStmt(pStmt);

				//generate new key and insert passphrase
				System.out.println("Setting KeyBox SSH public/private key pair");

				//generate application pub/pvt key and get values
				String passphrase = SSHUtil.keyGen();
				String publicKey = SSHUtil.getPublicKey();
				String privateKey = SSHUtil.getPrivateKey();

				//insert new keys
				pStmt = connection.prepareStatement("insert into application_key (public_key, private_key, passphrase) values(?,?,?)");
				pStmt.setString(1, publicKey);
				pStmt.setString(2, EncryptionUtil.encrypt(privateKey));
				pStmt.setString(3, EncryptionUtil.encrypt(passphrase));
				pStmt.execute();
				DBUtils.closeStmt(pStmt);

				System.out.println("KeyBox Generated Global Public Key:");
				System.out.println(publicKey);

				passphrase = null;
				publicKey = null;
				privateKey = null;

				//set config to default
				AppConfig.updateProperty("publicKey", "");
				AppConfig.updateProperty("privateKey", "");
				AppConfig.updateProperty("defaultSSHPassphrase", "${randomPassphrase}");
				
				//set to false
				AppConfig.updateProperty("resetApplicationSSHKey", "false");

			}

			//delete ssh keys
			SSHUtil.deletePvtGenSSHKey();


		} catch (Exception ex) {
            log.error(ex.toString(), ex);
		}

		DBUtils.closeStmt(statement);
		DBUtils.closeConn(connection);

		RefreshAuthKeyUtil.startRefreshAllSystemsTimerTask();
	}

}

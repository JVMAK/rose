/**
 * 
 */
package com.tazhi.rose.example.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Import;

import com.tazhi.rose.example.account.config.AccountTestConfiguration;

/**
 * @author Evan Wu
 *
 */
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@Import(AccountTestConfiguration.class)
public class AccountApplication {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(AccountApplication.class, args);
	}

}

/**
 * 
 */
package com.tazhi.rose.example.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Import;

import com.tazhi.rose.example.transfer.config.MoneyTransferTestConfiguration;
import com.tazhi.rose.example.transfer.config.MoneyTransferWebConfiguration;

/**
 * @author Evan Wu
 *
 */
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@Import({MoneyTransferTestConfiguration.class, MoneyTransferWebConfiguration.class})
public class MoneyTransferApplication {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(MoneyTransferApplication.class, args);
	}

}

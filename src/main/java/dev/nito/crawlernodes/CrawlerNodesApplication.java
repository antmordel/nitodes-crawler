package dev.nito.crawlernodes;

import dev.nito.crawlernodes.crawler.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@RequiredArgsConstructor
@SpringBootApplication
public class CrawlerNodesApplication implements CommandLineRunner {

	private final CrawlerService crawlerService;

	public static void main(String[] args) {
		SpringApplication.run(CrawlerNodesApplication.class, args);
	}

	@Override
	public void run(String... args) {
		crawlerService.start();
	}

}

package com.myData.dataLoader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.file.Files;
import javax.annotation.PostConstruct;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.myData.dataLoader.author.Author;
import com.myData.dataLoader.author.AuthorRepository;
import com.myData.dataLoader.book.Book;
import com.myData.dataLoader.book.BookRepository;

import connection.DataAstraProperties;

@SpringBootApplication
@EnableConfigurationProperties(DataAstraProperties.class)

public class DataLoaderApplication {

	@Autowired
	private AuthorRepository repo;
	
	@Autowired
	private BookRepository reppo;
	
	@Value("${datadump.author}")
	private String authorData;
	
	@Value("${datadump.works}")
	private String worksData;
	
	
	public static void main(String[] args) {
		SpringApplication.run(DataLoaderApplication.class, args);
	}
	
	public void initAuthors(){
		Path p=Paths.get(authorData);
		System.out.println(p.toString());
		try(Stream<String> lines=Files.lines(p)){
			lines.forEach(line->{
				String jsonString= line.substring(line.indexOf("{"));
				try {
					JSONObject jsonObject= new JSONObject(jsonString);
					Author author = new Author();
					author.setName(jsonObject.optString("name"));
					author.setId(jsonObject.optString("key").replace("/authors/", ""));
					repo.save(author);
				} catch (JSONException e) {
					e.printStackTrace();
				}				
			});
		
		} catch (IOException e ) {
			e.printStackTrace();
		}
		
	}
	
	@PostConstruct
	public void start() {
		initAuthors();
		initWorks();
		
	}
	
	
	private void initWorks() {
		Path p=Paths.get(worksData);
		System.out.println(p.toString());
		DateTimeFormatter format=DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
		try(Stream<String> lines=Files.lines(p)){
			lines.forEach(line->{
				String jsonString= line.substring(line.indexOf("{"));
				try {
					JSONObject jsonObject= new JSONObject(jsonString);
					Book book= new Book();
					book.setId(jsonObject.optString("key").replace("/works/", ""));
					book.setName(jsonObject.optString("title"));
					JSONObject desc = jsonObject.optJSONObject("description");
					if(desc!=null) {
						book.setDescription(desc.optString("value"));
					}
					JSONObject creat = jsonObject.optJSONObject("created");
					if(creat!=null) {
						String date= creat.optString("value");
						book.setPublishedDate(LocalDate.parse(date,format));
					}
					JSONArray cover = jsonObject.optJSONArray("covers");
					if(cover!=null) {
						List<String> coverIds= new ArrayList<>();
						for(int i=0;i<cover.length();i++) {
							coverIds.add(cover.getString(i));
						}
						book.setCoverIds(coverIds);
					}
					JSONArray authors = jsonObject.optJSONArray("authors");
					if(authors!=null) {
						List<String> authorIds= new ArrayList<>();
						for(int i=0;i<authors.length();i++) {
							String authorId=authors.getJSONObject(i).getJSONObject("author").getString("key").replace("/authors/", "");
							authorIds.add(authorId);
						}
						book.setAuthorIds(authorIds);
						List<String> authorNames= authorIds.stream().map(i-> repo.findById(i)).map(j-> {
							if(!j.isPresent()) {
								return "UnKnown Author";
							}
							else {
								return j.get().getName();
							}
						}).collect(Collectors.toList());
						book.setAuthorNames(authorNames);
					}
					
					reppo.save(book);
					
				}catch (JSONException e) {
					e.printStackTrace();
				}
				});
		} catch (IOException e ) {
			e.printStackTrace();
		}
		
	}

	@Bean
	public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataAstraProperties prop) {
		Path cloudConfigPath= prop.getSecureConnectBundle().toPath();
		return builder -> builder.withCloudSecureConnectBundle(cloudConfigPath);
	}
	
}

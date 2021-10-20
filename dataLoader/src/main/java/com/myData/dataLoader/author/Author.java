package com.myData.dataLoader.author;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.CassandraType.Name;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.annotation.Id;

@Table("author_by_id")
public class Author {
	
	@Id @PrimaryKeyColumn(name="author_id", ordinal=0, type=PrimaryKeyType.PARTITIONED)
	private String Id;
	
	@Column("author_name")
	@CassandraType(type = Name.TEXT)
	private String name;
	

	public String getId() {
		return Id;
	}

	public void setId(String id) {
		Id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	

}

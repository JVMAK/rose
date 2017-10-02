/**
 * 
 */
package com.tazhi.rose.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * R.O.S.E框架的JSON操作工具类。
 * 
 * @author Evan Wu
 *
 */
public class JsonUtils {
	public static ObjectMapper mapper;
	
	public static class Test {
		public Date now = new Date();
	}
	
	static {
		mapper = new ObjectMapper();
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	public static String toJson(Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static <T> T fromJson(String json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		return mapper.readValue(json, clazz);
	}
	public static <T> List<T> toList(String json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, clazz));
	}
	
	public static <K, V> Map<K, V> toMap(String json, Class<K> keyClazz, Class<V> ValueClazz) throws JsonParseException, JsonMappingException, IOException {
	    return mapper.readValue(json, mapper.getTypeFactory().constructMapType(HashMap.class, keyClazz, ValueClazz));
	}

    public static Object fromJson(String json, TypeReference<?> typeReference) throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(json, typeReference);
    }
}

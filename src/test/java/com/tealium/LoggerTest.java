package com.tealium;

import org.junit.Test;

public class LoggerTest {
	@Test
	public void test() throws Exception{
		new Logger(LogLevel.VERBOSE).log("Hello Test", LogLevel.VERBOSE);
	} 
}

package com.tealium;

import org.junit.Test;

public class LoggerTest {
	@Test
	public void test() throws Exception{
		new Logger(Logger.Level.VERBOSE).log("Hello Test", Logger.Level.VERBOSE);
	} 
}

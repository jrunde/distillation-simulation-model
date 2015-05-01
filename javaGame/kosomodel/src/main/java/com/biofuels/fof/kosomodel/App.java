package com.biofuels.fof.kosomodel;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        TestJsonConstruction testMessage = new TestJsonConstruction();
        String jsonString = testMessage.getJsonString();
		System.out.println(jsonString);
		
        System.out.println( "Hello World!" );
    }
}

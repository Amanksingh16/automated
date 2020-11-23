package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

public class Webscrap_city_states 
{
	public static void main(String [] args) throws IOException
	{
		URL servlet = new URL("https://en.wikipedia.org/wiki/List_of_cities_in_India_by_population");
		HttpsURLConnection conn = (HttpsURLConnection) servlet.openConnection();
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setRequestProperty("Content-type", "text/plain");
		
		BufferedReader reader = new BufferedReader( new InputStreamReader(conn.getInputStream()));
		
		String rowData = "";
		boolean dataStart = false;
		boolean isCity = false;
		Set<String> states = new HashSet<>();
		Set<String> cities = new HashSet<>();
		
		while(!(rowData = reader.readLine()).contains("id=\"See_also\">See also"))
		{
			if(rowData.contains("State or union territory"))
				dataStart = true;
			
			if(dataStart)
			{
				if(rowData.contains("<td>"))
				{
					if(rowData.contains("<a href="))
					{
						if(isCity == false)
						{
							isCity = true;
							String city = rowData.substring(rowData.indexOf("\">")+2, rowData.indexOf("</a>"));
							cities.add(city);
						}
						else
						{
							states.add(rowData.substring(rowData.indexOf("\">")+2, rowData.indexOf("</a>")));
							isCity = false;
						}
					}
				}
			}
		}
		System.out.println(cities);
		System.out.println(states);
	}
}

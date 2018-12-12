/**
 * Unlicensed code created by A Softer Space, 2018
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.powerLinker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.asofterspace.toolbox.io.CsvFile;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.web.WebAccessor;
import com.asofterspace.toolbox.Utils;


public class Main {
	
	public final static String PROGRAM_TITLE = "PowerLinker";
	public final static String VERSION_NUMBER = "0.0.0.1(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "11. December 2018";
	
	public final static String COOKIE = "";
	public final static String JSESSIONID = "";
	
	/**
	 * To use this program, we need to connect to linkedin as an authenticated user.
	 * The easiest way to do this is to go to:
	 *   https://www.linkedin.com/mynetwork/invite-connect/connections/
	 * Open the console and enter:
	 *   document.cookie
	 * Look around for something like:
	 *   JSESSIONID=\"ajax:4582453563422453434\"
	 * Paste the cookie (including ajax:) into the JSESSIONID variable
	 * Paste the entire document.cookie output into the COOKIE variable
	 * Now run this program (and do commit to git before deleting the cookie again!)
	 */
	public static void main(String[] args) {
		
		// let the Utils know in what program it is being used
		Utils.setProgramTitle(PROGRAM_TITLE);
		Utils.setVersionNumber(VERSION_NUMBER);
		Utils.setVersionDate(VERSION_DATE);
		
		CsvFile result = new CsvFile("connections.csv");
		
		List<String> headLine = new ArrayList<>();
		headLine.add("id");
		headLine.add("name");
		headLine.add("company");
		headLine.add("industry");
		result.setHeadLine(headLine);
		
		// now download the data about the connected users using something like
		// GET :: https://www.linkedin.com/voyager/api/relationships/connections?start=0&count=40&sortType=RECENTLY_ADDED
		// with the additional request header csrf-token: JSESSIONID
		
		Map<String, String> parameters = new HashMap<>();
		parameters.put("start", "0");
		parameters.put("count", "2000");
		parameters.put("sortType", "RECENTLY_ADDED");
		
		Map<String, String> extraHeaders = new HashMap<>();
		extraHeaders.put("Cookie", COOKIE);
		extraHeaders.put("csrf-token", JSESSIONID);
		
		String humans = WebAccessor.get("https://www.linkedin.com/voyager/api/relationships/connections", parameters, extraHeaders);
		
		JSON humanJs = new JSON(humans);
		
		// sooo humans now contains JSON like:
		// {"elements":[
		// {"createdAt":1644433221000,"entityUrn":"urn:li:fs_relConnection:weoijwfiwei","miniProfile":{"firstName":"Someone","lastName":"Someonesson","occupation":"--","objectUrn":"urn:li:member:549595010","entityUrn":"urn:li:fs_miniProfile:werwewrwer","publicIdentifier":"someone-someonesson","picture":...,"trackingId":"ljlmljnjnj=="}},
		// ...
		
		// aaand for each one we want to go to pages like:
		// https://www.linkedin.com/in/someone-someonesson
		// and find out what company the person is working for - and then also find out in what sector that company is active!
		
		List<JSON> humanArr = humanJs.getArray("elements");
		
		for (int i = 0; i < humanArr.size(); i++) {
		
			int id = i + 1;
			
			JSON miniHuman = humanArr.get(i).get("miniProfile");
			
			String name = miniHuman.getString("firstName") + " " + miniHuman.getString("lastName");
			
			String publicId = miniHuman.getString("publicIdentifier");
			
			String individualProfile = WebAccessor.get("https://www.linkedin.com/in/" + publicId + "/", null, extraHeaders);
			
			// the individual profile now contains info such as:
			// ... &quot;companyName&quot;:&quot;Azu Schlazzu&quot;,&quot;timePeriod&quot;:{&quot;startDate&quot;:{&quot;month&quot;:5,&quot;year&quot;:2015,&quot;$type&quot;:&quot;com.linkedin.common.Date&quot;},&quot;$type&quot;:&quot;com.linkedin.voyager.common.DateRange&quot;},&quot;company&quot;:{&quot;employeeCountRange&quot;:{&quot;start&quot;:11,&quot;end&quot;:50,&quot;$type&quot;:&quot;com.linkedin.voyager.identity.profile.EmployeeCountRange&quot;},&quot;industries&quot;:[&quot;Computer Software&quot;] ...
			// - bazinga! exactly what we are looking for!
			
			String company = "";
			String industry = "";
			String latestCompany = "";
			String latestIndustry = "";
			String latestCurrentCompany = "";
			String latestCurrentIndustry = "";
			
			// sooo there are several companies listed; we want to get the latest one, but if there is one without
			// and end time (so a current one) then we want to get that latest one without an end time
			// therefore, we keep track of the latest and the latest current one, and in the end if we have a
			// latest current one we take it and otherwise we take just the latest (non-current) one
			while (individualProfile.contains("&quot;companyName&quot;:&quot;")) {
				String newCompany = individualProfile.substring(individualProfile.indexOf("&quot;companyName&quot;:&quot;") + "&quot;companyName&quot;:&quot;".length());
				individualProfile = newCompany.substring(newCompany.indexOf("&quot;]"));
				if (newCompany.contains("com.linkedin.voyager.identity.profile.Position")) {
					newCompany = newCompany.substring(0, newCompany.indexOf("com.linkedin.voyager.identity.profile.Position"));
				}
				boolean isCurrent = !newCompany.contains("&quot;endDate&quot;");
				String newIndustry = "";
				if (newCompany.contains("&quot;industries&quot;:")) {
					newIndustry = newCompany.substring(newCompany.indexOf("&quot;industries&quot;:") + "&quot;industries&quot;:".length());
					newIndustry = newIndustry.substring(0, newIndustry.indexOf("&quot;]"));
					newIndustry = newIndustry.substring(7);
				}
				if (newCompany.contains("&quot;")) {
					newCompany = newCompany.substring(0, newCompany.indexOf("&quot;"));
				}
				latestCompany = newCompany;
				latestIndustry = newIndustry;
				if (isCurrent) {
					latestCurrentCompany = newCompany;
					latestCurrentIndustry = newIndustry;
				}
			}
			
			if (latestCurrentCompany.equals("")) {
				company = latestCompany;
				industry = latestIndustry;
			} else {
				company = latestCurrentCompany;
				industry = latestCurrentIndustry;
			}
			
			name = adjustStr(name);
			company = adjustStr(company);
			industry = adjustStr(industry);
			
			List<String> contentLine = new ArrayList<>();
			contentLine.add(""+id);
			contentLine.add(name);
			contentLine.add(company);
			contentLine.add(industry);
			result.appendContent(contentLine);
		}

		result.save();
	}
	
	private String adjustStr(String str) {
	
		str = str.replace("â€™", "'");
		str = str.replace("â‚¬", "€");
		str = str.replace("Ã¤", "ä");
		str = str.replace("Ã„", "Ä");
		str = str.replace("Ã¶", "ö");
		str = str.replace("Ã–", "Ö");
		str = str.replace("Ã¼", "ü");
		str = str.replace("Ãœ", "Ü");
		str = str.replace("Ã¡", "á");
		str = str.replace("Ã€", "À");
		str = str.replace("Ã©", "é");
		str = str.replace("Ã³", "ó");
		str = str.replace("Ã“", "Ó");
		str = str.replace("Ãº", "ú");
		str = str.replace("Ã­", "í");
		str = str.replace("Ã¦", "æ");
		str = str.replace("Ã†", "Æ");
		str = str.replace("Ã°", "ð");
		str = str.replace("Ã", "Ð");
		str = str.replace("Ã¾", "þ");
		str = str.replace("Ãž", "Þ");
		str = str.replace("ÃŸ", "ß");
		str = str.replace("Â¯", "¯");
		str = str.replace("â€“", "-");
		str = str.replace("&amp;", "&");
		str = str.replace("\n", "");
		str = str.replace("\r", "");
			
		return str;
	}
	
}

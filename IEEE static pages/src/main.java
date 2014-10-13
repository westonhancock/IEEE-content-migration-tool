import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.xpath.XPathExpression;
import org.apache.commons.lang3.StringEscapeUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class main {

	public static void main(String[] args) throws Exception {
		
		BufferedReader masterUrlFile = null;
		String[] blacklistUrls = null;
		ArrayList<String> outputtedUrls = null;
		
		CleanerProperties props = null;
		
		PrintWriter errorLog, omitElementsLog, formLog, duplicatePageLog, blacklistLog;
		String currentUrl = null;
		
		String xmlTF = "<?xml version=\"1.0\"?><root available-locales=\"en_US\" default-locale=\"en_US\"><dynamic-element dataType=\"boolean\" indexType=\"keyword\" name=\"sideNav\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"checkbox\" width=\"\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlBanner = "]]></dynamic-content></dynamic-element><dynamic-element dataType=\"string\" indexType=\"keyword\" name=\"pageName\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"text\" width=\"large\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlBannerMessage = "]]></dynamic-content></dynamic-element><dynamic-element dataType=\"string\" indexType=\"keyword\" name=\"pageDescription\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"text\" width=\"large\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlBodyContent = "]]></dynamic-content></dynamic-element><dynamic-element dataType=\"document-library\" fieldNamespace=\"ddm\" indexType=\"keyword\" name=\"backgroundImage\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"ddm-documentlibrary\" width=\"\"><dynamic-content language-id=\"en_US\"><![CDATA[]]></dynamic-content></dynamic-element><dynamic-element dataType=\"html\" fieldNamespace=\"ddm\" indexType=\"keyword\" name=\"bodyCopy\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"ddm-text-html\" width=\"large\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlAddInfo = "]]></dynamic-content></dynamic-element><dynamic-element dataType=\"html\" fieldNamespace=\"ddm\" indexType=\"keyword\" name=\"additionalInformation\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"ddm-text-html\" width=\"large\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlRelated = "]]></dynamic-content></dynamic-element><dynamic-element dataType=\"html\" fieldNamespace=\"ddm\" indexType=\"keyword\" name=\"relatedLinks\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"ddm-text-html\" width=\"large\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlClose = "]]></dynamic-content></dynamic-element></root>";
		
		errorLog = null;
		omitElementsLog = null;
		formLog = null;
		duplicatePageLog = null;
		blacklistLog = null;

		try
		{
			// open master url list file
			masterUrlFile = new BufferedReader(new FileReader("C:/Users/Liferay/Desktop/ieee_test/masterUrlList.txt"));
			// open blacklist list file
			File tempFile = new File("C:/Users/Liferay/Desktop/ieee_test/blacklist.txt");
			
		    FileInputStream tempInputStream = new FileInputStream(tempFile);
		    byte[] tempData = new byte[(int)tempFile.length()];
		    tempInputStream.read(tempData);
		    tempInputStream.close();
		    
		    blacklistUrls = (new String(tempData, "UTF-8")).split(",");
		    
		    // rid any leading or trailing whitespace in urls
		    for (int i = 0; i < blacklistUrls.length; ++i)
		    {
		    	blacklistUrls[i] = blacklistUrls[i].trim();
		    }
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			System.exit(1);
		}
		
		errorLog = new PrintWriter(new FileWriter("C:/Users/Liferay/Desktop/ieee_test/logs/errorLog.txt", true));
		omitElementsLog = new PrintWriter(new FileWriter("C:/Users/Liferay/Desktop/ieee_test/logs/omitElementsLog.txt", true));
		formLog = new PrintWriter(new FileWriter("C:/Users/Liferay/Desktop/ieee_test/logs/formLog.txt", true));
		duplicatePageLog = new PrintWriter(new FileWriter("C:/Users/Liferay/Desktop/ieee_test/logs/duplicatePageLog.txt", true));
		blacklistLog = new PrintWriter(new FileWriter("C:/Users/Liferay/Desktop/ieee_test/logs/blacklistLog.txt", true));
		
		outputtedUrls = new ArrayList<String>();
		
		// set some properties to non-default values
		props = new CleanerProperties();
		
		props.setAdvancedXmlEscape(true);
		props.setTransResCharsToNCR(true);
		props.setRecognizeUnicodeChars(false);
		props.setIgnoreQuestAndExclam(false);
		props.setOmitXmlDeclaration(true);
		
		int urlIndex = 0;
		
		while ((currentUrl = masterUrlFile.readLine()) != null)
		{
			++urlIndex;
			System.out.println(urlIndex);
			
			TagNode root;
			TagNode subnavNode, alignNode, contentTypeSimpleNode, addInfoNode, sideBoxNode, sideBoxSectionsNode, textContentNode, relatedLinksNode;
			TagNode bodyContentNode;
			String bodyContentStr, relatedLinksStr, addInfoStr, subnavStr, bannerHeaderStr, bannerTextStr, masterPageStr;
			String outputFileName;
			String[] currentUrlSplitArray;
			boolean blacklisted = false;
			
			root = null;
			subnavNode = null;
			alignNode = null;
			contentTypeSimpleNode = null;
			addInfoNode = null;
			sideBoxNode = null;
			sideBoxSectionsNode = null;
			textContentNode = null;
			relatedLinksNode = null;
			bodyContentNode = null;
			
			bodyContentStr = "";
			relatedLinksStr = "";
			addInfoStr = "";
			subnavStr = "";
			bannerHeaderStr = "";
			bannerTextStr = "";
			masterPageStr = "";

			outputFileName = "";
			
			/*** check currentUrl against blacklist ***/
			currentUrlSplitArray = currentUrl.replaceAll("http://standards.ieee.org/", "").split("/");
			
			for (String tempString : blacklistUrls)
			{
				String[] tempStringSplitArray = tempString.replaceAll("http://standards.ieee.org/", "").split("/");
				
				if (tempStringSplitArray.length > currentUrlSplitArray.length)
				{
					// currentUrl is above the blacklist url (directory-wise) so currentUrl is not blacklisted
					continue;
				}
				
				for (int i = 0; i < tempStringSplitArray.length; ++i)
				{
					if ( ! tempStringSplitArray[i].equals(currentUrlSplitArray[i]))
					{
						// currentUrl didn't match blacklist url (starting at the base, directory-wise) so currentUrl is not blacklisted
						break;
					}
					else
					{
						// at very end of blacklist url string split array
						if (i == (tempStringSplitArray.length - 1))
						{
							// every word in blacklist url matched currentUrl so currentUrl is blacklisted
							blacklisted = true;
						}
					}
				}
				
				if (blacklisted == true)
				{
					break;
				}
			}
			
			if (blacklisted == true)
			{
				blacklistLog.println(currentUrl);
				continue;
			}
			
			/*** format output file name ***/
			// if url had "/" at its end (e.g. http://google.com/ ), the string array will store an extra element at its tail which actually has nothing in it. so rid this. 
			while (currentUrlSplitArray[currentUrlSplitArray.length - 1].equals(""))
			{
				currentUrlSplitArray = Arrays.copyOfRange(currentUrlSplitArray, 0, currentUrlSplitArray.length - 1);
			}
			
			if (currentUrlSplitArray[currentUrlSplitArray.length - 1].equals("index.html"))
			{
				currentUrlSplitArray = Arrays.copyOfRange(currentUrlSplitArray, 0, currentUrlSplitArray.length - 1);
				currentUrlSplitArray[currentUrlSplitArray.length - 1] += ".xml";
			}
			
			currentUrlSplitArray[currentUrlSplitArray.length - 1] = currentUrlSplitArray[currentUrlSplitArray.length - 1].replaceAll(".html", ".xml");
			
			if (( ! currentUrlSplitArray[currentUrlSplitArray.length -1].contains(".html")) && ( ! currentUrlSplitArray[currentUrlSplitArray.length -1].contains(".xml")))
			{
				currentUrlSplitArray[currentUrlSplitArray.length -1] += ".xml";
			}
			
			for (int i = 0; i < currentUrlSplitArray.length; ++i)
			{
				// not at end of list
				if (i < currentUrlSplitArray.length - 1)
				{
					outputFileName += ((currentUrlSplitArray[i]) + "-"); // this dash is a special dash. only this dash works in filenames. copy and paste if using it (don't type it in).
				}
				else // at end of list
				{
					outputFileName += currentUrlSplitArray[i];
				}
			}

			// check if file already exists.. if it does exist, it's most likely due to index.html page existing as a duplicate
			if (outputtedUrls.contains(outputFileName))
			{
				duplicatePageLog.println(outputFileName + " generated from " + currentUrl);
				continue;
			}
	
			/*** parse url ***/
			try
			{
				root = new HtmlCleaner(props).clean(
				    new URL(currentUrl)
				);
			}
			catch (Exception e)
			{
				// if can't parse url for some reason (e.g. url doesn't exist).. move on to the next url
				errorLog.println(currentUrl + " - " + e.getMessage());
				continue;
			}
			
			/*** grab necessary html nodes ***/
			Object[] formList = root.evaluateXPath("//div[@id='text-content']/form");
			Object[] formList2 = root.evaluateXPath("//div[@class='alignleft']/form");
			if (formList.length > 0 || formList2.length > 0 )
			{
				formLog.println(currentUrl + " - form");
			}
			
			Object[] contentTypeSimpleList = root.evaluateXPath("//div[@id='content-type-simple']");
			if (contentTypeSimpleList.length > 0 || contentTypeSimpleList.length > 0 )
			{
				contentTypeSimpleNode = (TagNode)contentTypeSimpleList[0];
			}

			Object[] bannerHeaderList = root.evaluateXPath("//div[@id='title-banner']/h1[1]/text()");
			if (bannerHeaderList.length > 0)
			{
				bannerHeaderStr = bannerHeaderList[0].toString();
			}

			Object[] bannerTextList = root.evaluateXPath("//div[@id='title-banner']/p[1]/text()");
			if (bannerTextList.length > 0)
			{
				bannerTextStr = bannerTextList[0].toString();
			}

			Object[] subnavList = root.evaluateXPath("//div[@id='content-subnav']");
			if (subnavList.length > 0)
			{
				subnavNode = (TagNode)subnavList[0];
				subnavNode.getParent().removeChild(subnavNode);
				
				subnavStr = "true";
			}
			else
			{
				subnavStr = "false";
			}
			
			Object[] alignList = root.evaluateXPath("//div[@class='alignleft']");
			if (alignList.length > 0)
			{
				alignNode = (TagNode)alignList[0];
			}
			
			Object[] sideBoxSectionsList = root.evaluateXPath("//div[@class='side-box']//h4");
			if (sideBoxSectionsList.length > 0)
			{
				for (int i = 0; i< sideBoxSectionsList.length; i++)
				{
					sideBoxSectionsNode = (TagNode)sideBoxSectionsList[i];
					String sideBoxSectionsStr = (sideBoxSectionsNode.getText()).toString();
					if (sideBoxSectionsStr.equalsIgnoreCase("related links"))
					{
						relatedLinksNode = sideBoxSectionsNode.getParent();
					}
					else if (sideBoxSectionsStr.equalsIgnoreCase("additional information"))
					{
						addInfoNode = sideBoxSectionsNode.getParent();
					}
					else 
					{
						omitElementsLog.println(currentUrl + " - " + sideBoxSectionsStr);
					}
				}
			}
			
			Object[] sideBoxList = root.evaluateXPath("//div[@class='side-box']");
			if (sideBoxList.length > 0)
			{
				sideBoxNode = (TagNode)sideBoxList[0]; 
			    sideBoxNode.getParent().removeChild(sideBoxNode);
			}
			
			Object[] textContentList = root.evaluateXPath("//div[@id='text-content']");
			if (textContentList.length > 0)
			{
				textContentNode = (TagNode)textContentList[0];
			}
			
			/*** page layout possibilities logic ***/
			if (alignNode != null)
			{
				if (textContentNode != null)
				{
					bodyContentNode = textContentNode;
				}
				else
				{
					bodyContentNode = alignNode;
				}
			}
			else
			{
				if (textContentNode != null)
				{
					bodyContentNode = textContentNode;
				}
				else if (contentTypeSimpleNode != null)
				{
					bodyContentNode = contentTypeSimpleNode;
				}
			}
			
			if (bodyContentNode == null)
			{
				errorLog.println(currentUrl + " - unable to find body content");
			    continue;
			}
			
			/*** grab xml in text form of body, related links, and additional info elements ***/
			if (bodyContentNode != null)
			{
				new PrettyXmlSerializer(props).writeToFile(
				    bodyContentNode, "C:/Users/Liferay/Desktop/ieee_test/body_content.xml", "utf-8"
				);
				
				BufferedReader tempReader = new BufferedReader(new FileReader("C:/Users/Liferay/Desktop/ieee_test/body_content.xml"));
				String tempString;
				
				while ((tempString = tempReader.readLine()) != null)
				{
					bodyContentStr += tempString;
				}
				
				tempReader.close();
				
				Files.delete(Paths.get("C:/Users/Liferay/Desktop/ieee_test/body_content.xml"));
			}
			
			if (relatedLinksNode != null)
			{
				new PrettyXmlSerializer(props).writeToFile(
				    relatedLinksNode, "C:/Users/Liferay/Desktop/ieee_test/related_links.xml", "utf-8"
				);
				
				BufferedReader tempReader = new BufferedReader(new FileReader("C:/Users/Liferay/Desktop/ieee_test/related_links.xml"));
				String tempString;
				
				while ((tempString = tempReader.readLine()) != null)
				{
					relatedLinksStr += tempString;
				}
				
				tempReader.close();
				
				Files.delete(Paths.get("C:/Users/Liferay/Desktop/ieee_test/related_links.xml"));
			}
			
			if (addInfoNode != null)
			{
				new PrettyXmlSerializer(props).writeToFile(
				    addInfoNode, "C:/Users/Liferay/Desktop/ieee_test/add_info.xml", "utf-8"
				);
				
				BufferedReader tempReader = new BufferedReader(new FileReader("C:/Users/Liferay/Desktop/ieee_test/add_info.xml"));
				String tempString;
				
				while ((tempString = tempReader.readLine()) != null)
				{
					addInfoStr += tempString;
				}
				
				tempReader.close();
				
				Files.delete(Paths.get("C:/Users/Liferay/Desktop/ieee_test/add_info.xml"));
			}

			/*** condense all strings, format them to look nice, and spit them out to file ***/
			{
				masterPageStr = xmlTF + subnavStr + xmlBanner + bannerHeaderStr + xmlBannerMessage + bannerTextStr + xmlBodyContent + bodyContentStr + xmlRelated + relatedLinksStr + xmlAddInfo + addInfoStr + xmlClose;
	
				// do parsing
				TagNode rootNodeUnformatted = new HtmlCleaner(props).clean(masterPageStr);
				
				// serialize to xml file
				new PrettyXmlSerializer(props).writeToFile(
				    rootNodeUnformatted, "C:/Users/Liferay/Desktop/masterPageCleaned.xml", "utf-8"
				);
				
				BufferedReader tempReader = new BufferedReader(new FileReader("C:/Users/Liferay/Desktop/masterPageCleaned.xml"));
				
				PrintWriter tempWriter = new PrintWriter(new FileWriter("C:/Users/Liferay/Desktop/ieee_test/xml_files/" + outputFileName, true));
				tempWriter.println("<?xml version=\"1.0\"?>");
				tempWriter.println("");
				
				String tempString = null;
				
				// skip all lines until keyword "root" appears to avoid tags like "<html>" and "<body>"
				while ((tempString = tempReader.readLine()) != null)
				{
					if (tempString.contains("<root available-locales=\"en_US\" default-locale=\"en_US\">"))
					{
						tempWriter.println(tempString.replaceFirst("\t", "").replaceFirst("\t", ""));
						break;
					}
				}
				
				while ((tempString = tempReader.readLine()) != null)
				{					
					tempString = StringEscapeUtils.unescapeHtml4(tempString.replaceFirst("\t", "").replaceFirst("\t", ""));
					
					if (tempString.contains("</root>"))
					{
						tempWriter.print(tempString);
						break;
					}
					else
					{
						tempWriter.println(tempString);
					}
				}
				
				tempReader.close();
				tempWriter.close();
				Files.delete(Paths.get("C:/Users/Liferay/Desktop/masterPageCleaned.xml"));
				
				outputtedUrls.add(outputFileName);
			}
		}
		
		masterUrlFile.close();
		errorLog.close();
		omitElementsLog.close();
		formLog.close();
		duplicatePageLog.close();
		blacklistLog.close();
	}
}

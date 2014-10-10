import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.File;
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
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;

import java.io.IOException;

public class main {

	public static void main(String[] args) throws Exception {
		
		BufferedReader masterUrlFile = new BufferedReader(new FileReader("C:/Users/Liferay/Desktop/ieee_test/test.txt"));
		PrintWriter errorLog, blacklistLog, omitElementsLog, formLog, attentionLog;
		String currentUrl = null;
		
		String xmlTF = "<?xml version=\"1.0\"?><root available-locales=\"en_US\" default-locale=\"en_US\"><dynamic-element dataType=\"boolean\" indexType=\"keyword\" name=\"sideNav\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"checkbox\" width=\"\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlBanner = "]]></dynamic-content></dynamic-element><dynamic-element dataType=\"string\" indexType=\"keyword\" name=\"pageName\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"text\" width=\"large\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlBannerMessage = "]]></dynamic-content></dynamic-element><dynamic-element dataType=\"string\" indexType=\"keyword\" name=\"pageDescription\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"text\" width=\"large\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlBodyContent = "]]></dynamic-content></dynamic-element><dynamic-element dataType=\"document-library\" fieldNamespace=\"ddm\" indexType=\"keyword\" name=\"backgroundImage\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"ddm-documentlibrary\" width=\"\"><dynamic-content language-id=\"en_US\"><![CDATA[]]></dynamic-content></dynamic-element><dynamic-element dataType=\"html\" fieldNamespace=\"ddm\" indexType=\"keyword\" name=\"bodyCopy\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"ddm-text-html\" width=\"large\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlAddInfo = "]]></dynamic-content></dynamic-element><dynamic-element dataType=\"html\" fieldNamespace=\"ddm\" indexType=\"keyword\" name=\"additionalInformation\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"ddm-text-html\" width=\"large\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlRelated = "]]></dynamic-content></dynamic-element><dynamic-element dataType=\"html\" fieldNamespace=\"ddm\" indexType=\"keyword\" name=\"relatedLinks\" readOnly=\"false\" repeatable=\"false\" required=\"false\" showLabel=\"true\" type=\"ddm-text-html\" width=\"large\"><dynamic-content language-id=\"en_US\"><![CDATA[";
		String xmlClose = "]]></dynamic-content></dynamic-element></root>";
		
		errorLog = null;
		blacklistLog = null;
		omitElementsLog = null;
		formLog = null;
		
		errorLog = new PrintWriter(new FileWriter("errorLogUrl", true));
		// blacklistLog = new PrintWriter(new FileWriter("blacklistLogUrl", true));
		omitElementsLog = new PrintWriter(new FileWriter("omitElementsLogUrl", true));
		formLog = new PrintWriter(new FileWriter("C:/Users/Liferay/Desktop/ieee_test/formLogUrl.txt", true));
		// attentionLog....
		
		while ((currentUrl = masterUrlFile.readLine()) != null)
		{
			CleanerProperties props = new CleanerProperties();
			TagNode subnavNode, alignNode, addInfoNode, sideBoxNode, contentNode, textContentNode, relatedLinksNode, searchBoxNode, searchContentNode;
			TagNode bodyContentNode;
			String bodyContentStr, relatedLinksStr, addInfoStr, subnavStr, bannerHeaderStr, bannerTextStr, masterPageStr;
			
			subnavNode = null;
			alignNode = null;
			addInfoNode = null;
			sideBoxNode = null;
			contentNode = null;
			textContentNode = null;
			relatedLinksNode = null;
			searchBoxNode = null;
			bodyContentNode = null;
			searchContentNode = null;
			
			bodyContentStr = "";
			relatedLinksStr = "";
			addInfoStr = "";
			subnavStr = "";
			bannerHeaderStr = "";
			bannerTextStr = "";
			masterPageStr = "";
			
			// set some properties to non-default values
			props.setTransResCharsToNCR(true);
			props.setRecognizeUnicodeChars(true);
			props.setIgnoreQuestAndExclam(false);
			props.setOmitXmlDeclaration(true);
	
			// do parsing
			TagNode root = new HtmlCleaner(props).clean(
			    new URL(currentUrl)
			);
			
			Object[] formList = root.evaluateXPath("//div[@id='text-content']/form");
			Object[] formList2 = root.evaluateXPath("//div[@class='alignleft']/form");
			if (formList.length > 0 || formList2.length > 0 )
			{
				formLog.println(currentUrl + " - form");
				continue;
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
			
			Object[] relatedLinksList = root.evaluateXPath("//h4[text()='RELATED LINKS']");
			Object[] relatedLinksList2 = root.evaluateXPath("//h4[text()='Related Links']");
			if (relatedLinksList.length > 0)
			{
				relatedLinksNode = ((TagNode)relatedLinksList[0]).getParent();
			}
			else if (relatedLinksList2.length > 0)
			{
				relatedLinksNode = ((TagNode)relatedLinksList2[0]).getParent();
			}
			
			Object[] addInfoList = root.evaluateXPath("//h4[text()='ADDITIONAL INFORMATION']");
			Object[] addInfoList2 = root.evaluateXPath("//h4[text()='Additional Information']");
			if (addInfoList.length > 0)
			{
				addInfoNode = ((TagNode)addInfoList[0]).getParent();
			}
			else if (addInfoList2.length > 0)
			{
				addInfoNode = ((TagNode)addInfoList2[0]).getParent();
			}
			
			Object[] alignList = root.evaluateXPath("//div[@class='alignleft']");
			if (alignList.length > 0)
			{
				alignNode = (TagNode)alignList[0];
			}
			
			Object[] sideBoxList = root.evaluateXPath("//div[@id='side-box']");
			if (sideBoxList.length > 0)
			{
				sideBoxNode = (TagNode)sideBoxList[0];
				sideBoxNode.getParent().removeChild(sideBoxNode);
			}
			
			Object[] sideBoxSectionsList = root.evaluateXPath("//div[@class='side-box']//h4/text()");
			if (sideBoxSectionsList.length > 0)
			{
				for (int i = 0; i< sideBoxSectionsList.length; i++)
				{
					String sideBoxSectionsStr = sideBoxSectionsList[i].toString();
			    
					if (!sideBoxSectionsStr.equalsIgnoreCase("related links") && !sideBoxSectionsStr.equalsIgnoreCase("additional information"))
					{
						omitElementsLog.println(currentUrl + " - " + sideBoxSectionsStr);
					}
				}
			}
			
			Object[] searchBoxList = root.evaluateXPath("//div[@id='search-box']");
			if (searchBoxList.length > 0)
			{
				searchBoxNode = (TagNode)searchBoxList[0];
			}

			/*Object[] searchContentList = root.evaluateXPath("//div[@id='content-type-search-result-content']");
			if (searchContentList.length > 0)
			{
				searchContentNode = (TagNode)searchContentList[0];
				// will be blacklisted
			}*/
			
			Object[] textContentList = root.evaluateXPath("//div[@id='text-content']");
			if (textContentList.length > 0)
			{
				textContentNode = (TagNode)textContentList[0];
			}
			
			
			// find inline icon images and replace src url
			/*
			Object[] linkImgList = root.getElementsByAttValue("src", "/images/external-link.gif", true, false);
			if (linkImgList.length > 0)
			{
				TagNode linkImgNode = (TagNode)linkImgList[0];
				linkImgNode.removeAttribute("src");
				linkImgNode.addAttribute("src", "[$FILE=external-link.gif$]");
			}
			Object[] protectedLinkImgList = root.getElementsByAttValue("src", "/images/external-link-protected.gif", true, false);
			if (protectedLinkImgList.length > 0)
			{
				TagNode protectedLinkImgNode = (TagNode)protectedLinkImgList[0];
				protectedLinkImgNode.removeAttribute("src");
				protectedLinkImgNode.addAttribute("src", "[$FILE=external-link-protected.gif$]");
			}
			Object[] pdfImgList = root.getElementsByAttValue("src", "/images/pdficon.gif", true, false);
			if (pdfImgList.length > 0)
			{
				TagNode pdfImgNode = (TagNode)pdfImgList[0];
				pdfImgNode.removeAttribute("src");
				pdfImgNode.addAttribute("src", "[$FILE=pdficon.gif$]");
			}
			*/
			
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
				else
				{
					errorLog.println(currentUrl + " - unable to find body content");
					continue;
				}
			}
			
			/* grab xml in text form of body, related links, and additional info elements */
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

			{
				masterPageStr = xmlTF + subnavStr + xmlBanner + bannerHeaderStr + xmlBannerMessage + bannerTextStr + xmlBodyContent + bodyContentStr + xmlRelated + relatedLinksStr + xmlAddInfo + addInfoStr + xmlClose;
	
				// do parsing
				TagNode rootNodeUnformatted = new HtmlCleaner(props).clean(masterPageStr);
				
				
				// serialize to xml file
				new PrettyXmlSerializer(props).writeToFile(
				    rootNodeUnformatted, "C:/Users/Liferay/Desktop/masterPageFormatted.xml", "utf-8"
				);
				
				BufferedReader tempReader = new BufferedReader(new FileReader("C:/Users/Liferay/Desktop/masterPageFormatted.xml"));
				String tempString = null;
				
				PrintWriter tempWriter = new PrintWriter(new FileWriter("C:/Users/Liferay/Desktop/ieee_test/masterPage.xml", true));
				tempWriter.println("<?xml version=\"1.0\"?>");
				tempWriter.println("");
				
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
					tempString = tempString.replaceFirst("\t", "").replaceFirst("\t", "").replaceAll("</html>","").replaceAll("</body>", "").replaceAll("&#60;", "<").replaceAll("&#62;", ">");
					tempWriter.println(tempString);
				}
				
				tempReader.close();
				tempWriter.close();
			}
			
			break;
		}
		/*
		errorLog.close();
		blacklistLog.close();
		omitElementsLog.close();
		formLog.close();
		masterUrlFile.close();
		*/

		errorLog.close();
		formLog.close();
		masterUrlFile.close();
		omitElementsLog.close();
	}
}

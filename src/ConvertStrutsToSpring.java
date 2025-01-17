import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConvertStrutsToSpring {

    private static final String SPRING_LIB =
            "<%@taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\"%>\n"
                    + "<%@ taglib prefix=\"spring\" uri=\"http://www.springframework.org/tags\"%>\n";

    private static final Map<String, String> CONVERSION_MAP = new LinkedHashMap<>() {{
        // Tag libraries
        put("<%@ taglib prefix=\"s\" uri=\"/struts-tags\" %>",
                "<%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\" %>");
        put("<%@ taglib prefix=\"f\" uri=\"/struts-tags\" %>",
                "<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>");

        // Form tags
        put("<s:form action=\"", "<form:form action=\"");
        put("</s:form>", "</form:form>");
        put("<s:textfield name=\"", "<form:input path=\"");
        put("<s:password name=\"", "<form:password path=\"");
        put("<s:textarea name=\"", "<form:textarea path=\"");
        put("<s:checkbox name=\"", "<form:checkbox path=\"");
        put("<s:radio name=\"", "<form:radiobutton path=\"");
        put("<s:hidden name=\"", "<form:hidden path=\"");
        put("<s:submit>", "<form:button>");
        put("</s:submit>", "</form:button>");

        // Display data
        put("<s:property value=\"", "${");
        put("\" />", "}");

        // URL tags
        put("<s:url value=\"", "<c:url value=\"");
        put("</s:url>", "</c:url>");
        put("<s:param name=\"", "<c:param name=\"");

        // Errors
        put("<s:fielderror>", "<form:errors path=\"");
        put("</s:fielderror>", "</form:errors>");
        put("<s:actionerror>",
                "<c:forEach items=\"${errors}\" var=\"error\"><li>${error}</li></c:forEach>");
        put("<s:actionmessage>",
                "<c:forEach items=\"${messages}\" var=\"message\"><li>${message}</li></c:forEach>");

        // If-Else conditions
        put("<s:if test=\"", "<c:if test=\"${");
        put("</s:if>", "</c:if>");
        put("<s:else>", "<c:if test=\"${!(...)}\">");
        put("</s:else>", "</c:if>");

        // Messages
        put("<s:text name=\"", "<spring:message code=\"");
        put("</s:text>", "</spring:message>");
    }};

    private static final List<Pattern> REGEX_PATTERNS = List.of(
            Pattern.compile("\\$\\{parameters\\.(.*?)\\}"),
            // ${parameters.paramName} -> ${param.paramName}
            Pattern.compile("\\$\\{action\\.(.*?)\\}"),
            // ${action.property} -> ${model.property}
            Pattern.compile("\\$\\{(.*?)\\.size\\}"),
            // ${list.size} -> ${fn:length(list)} (cần JSTL fn)
            Pattern.compile("\\%\\{(.*?)\\}"),              // %{condition} -> ${condition}
            Pattern.compile("\\$\\{f:url\\('(.*?)'\\)\\}"),
            // ${f:url('/css/style.css')} -> <c:url value='/css/style.css'/>
            //custom
            Pattern.compile("<html:hidden\\s+property=\"(.*?)\"/>"),
            Pattern.compile("<html:text\\s+property=\"(.*?)\"\\s+styleId=\"(.*?)\"\\s+styleClass=\"(.*?)\"\\s+errorStyleClass=\"(.*?)\""),
            Pattern.compile("<html:password\\s+property=\"(.*?)\"\\s+styleClass=\"(.*?)\"\\s+errorStyleClass=\"(.*?)\""),
            Pattern.compile("<html:errors property=\"(.*?)\""),
            Pattern.compile("<s:(.*?)\\s+styleClass=\"(.*?)\"\\s+property=\"(.*?)\"\\s+value=\"(.*?)\"\\s+/>"),
            Pattern.compile("<html:hidden\\s+property=\"(.*?)\"\\s+styleId=\"(.*?)\"")
    );

    private static final Map<String, String> CONVERSION_MAP_CUSTOM = new LinkedHashMap<>() {{
        put("<s:form>", "<form:form method=\"post\" modelAttribute=\"changeThisFormName\">");
        put("</s:form>", "</form:form>");
    }};

    private static String convertJSPContent(String content) {

        content = SPRING_LIB + content;

        for (Map.Entry<String, String> entry : CONVERSION_MAP.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
        }

        for (Pattern pattern : REGEX_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String replacement = matcher.group(1);
                if (pattern.pattern().equals("\\$\\{parameters\\.(.*?)\\}")) {
                    matcher.appendReplacement(buffer, "\\$\\{param." + replacement + "\\}");
                } else if (pattern.pattern().equals("\\$\\{action\\.(.*?)\\}")) {
                    matcher.appendReplacement(buffer, "\\$\\{model." + replacement + "\\}");
                } else if (pattern.pattern().equals("\\$\\{(.*?)\\.size\\}")) {
                    matcher.appendReplacement(buffer, "\\$\\{fn:length(" + replacement + ")\\}");
                } else if (pattern.pattern().equals("\\%\\{(.*?)\\}")) {
                    matcher.appendReplacement(buffer, "\\$\\{" + replacement + "\\}");
                } else if (pattern.pattern().equals("\\$\\{f:url\\('(.*?)'\\)\\}")) {
                    matcher.appendReplacement(buffer, "<c:url value='" + replacement + "'/>");
                }
            }
            matcher.appendTail(buffer);
            content = buffer.toString();
        }

        return content;
    }

    private static String convertJSPContentCustom(String content) {

        content = SPRING_LIB + removeStrutsLines(content);

        for (Map.Entry<String, String> entry : CONVERSION_MAP_CUSTOM.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
        }

        for (Pattern pattern : REGEX_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                switch (pattern.pattern()) {
                    case "\\$\\{parameters\\.(.*?)\\}":
                        matcher.appendReplacement(buffer, "\\$\\{param.$1\\}");
                        break;
                    case "\\$\\{action\\.(.*?)\\}":
                        matcher.appendReplacement(buffer, "\\$\\{model.$1\\}");
                        break;
                    case "\\$\\{(.*?)\\.size\\}":
                        matcher.appendReplacement(buffer, "\\$\\{fn:length($1)\\}");
                        break;
                    case "\\%\\{(.*?)\\}":
                        matcher.appendReplacement(buffer, "\\$\\{$1\\}");
                        break;
                    case "\\$\\{f:url\\('(.*?)'\\)\\}":
                        matcher.appendReplacement(buffer, "..$1");
                        break;
                    case "<html:hidden\\s+property=\"(.*?)\"/>":
                        matcher.appendReplacement(buffer, "<input type=\"hidden\" name=\"$1\" value=\"\\$\\{$1\\}\"/>");
                        break;
                    case "<html:text\\s+property=\"(.*?)\"\\s+styleId=\"(.*?)\"\\s+styleClass=\"(.*?)\"\\s+errorStyleClass=\"(.*?)\"":
                        matcher.appendReplacement(buffer, "<form:input path=\"$1\" id=\"$2\" cssClass=\"$3\"");
                        break;
                    case "<html:password\\s+property=\"(.*?)\"\\s+styleClass=\"(.*?)\"\\s+errorStyleClass=\"(.*?)\"":
                        matcher.appendReplacement(buffer, "<form:password path=\"$1\" cssClass=\"$2\"");
                        break;
                    case "<html:errors property=\"(.*?)\"":
                        matcher.appendReplacement(buffer, "<form:errors path=\"$1\"");
                        break;
                    case "<s:(.*?)\\s+styleClass=\"(.*?)\"\\s+property=\"(.*?)\"\\s+value=\"(.*?)\"\\s+/>":
                        matcher.appendReplacement(buffer, "<button type=\"$1\" class=\"$2\" name=\"$3\">$4</button>");
                        break;
                    case "<html:hidden\\s+property=\"(.*?)\"\\s+styleId=\"(.*?)\"":
                        matcher.appendReplacement(buffer, "<form:hidden path=\"$1\" id=\"$2\"");
                        break;
                    default:
                        break;
                }
            }
            matcher.appendTail(buffer);
            content = buffer.toString();
        }

        return content;
    }

    private static String removeStrutsLines(String content) {
        return content.replaceAll("(?m)^.*struts.*(?:\\r?\\n)?", "");
    }

    private static String readFile(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }


    private static void writeFile(String filePath, String content) throws IOException {
        Files.writeString(Paths.get(filePath), content);
    }

    public static void main(String[] args) {

        ClassLoader classLoader = ConvertStrutsToSpring.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("file_convert/struts.jsp");

        String outputFilePath = "src/file_convert/spring.jsp";

        try {
            String content = readFile(inputStream);

            String convertedContent = convertJSPContentCustom(content);

            writeFile(outputFilePath, convertedContent);

            System.out.println("Done");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

}

<%@ page import="org.apache.log4j.Logger" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
  <title>test logging</title>
</head>
<body>

    <%

    Logger log = Logger.getLogger("testLogger");
    log.error("log from test logging jsp");
    %>
    logged to log4j file


</body>
</html>

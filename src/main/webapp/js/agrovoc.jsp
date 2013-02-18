<!DOCTYPE html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title>Test</title>
    <link href="//netdna.bootstrapcdn.com/twitter-bootstrap/2.3.0/css/bootstrap-combined.min.css" rel="stylesheet">
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
    <script src="//netdna.bootstrapcdn.com/twitter-bootstrap/2.3.0/js/bootstrap.min.js"></script>
    <script src="http://168.202.48.143:8080/agrovoc/js/jquery.agrovoc.js"></script>
</head>
<body>
<h1>Test</h1>

<form action="agrovoc.jsp" method="post">
    <label>
        Query:
        <input name="terms" type="text" autocomplete="off"
               data-provide="agrovoc"
               data-url="http://168.202.48.143:8080/agrovoc"
               data-codes="${fn:join(paramValues['terms'],', ')}"
               data-items="8">
    </label>
    <input type="submit" value="Submit">
</form>
</body>
</html>
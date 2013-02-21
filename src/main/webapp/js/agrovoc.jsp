<!DOCTYPE html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title>Test</title>
    <link href="//netdna.bootstrapcdn.com/twitter-bootstrap/2.3.0/css/bootstrap-combined.min.css" rel="stylesheet">
    <link href="../css/css3pie-bootstrap.css" rel="stylesheet">
    <link href="../css/agrovoc.css" rel="stylesheet">
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
    <script src="//netdna.bootstrapcdn.com/twitter-bootstrap/2.3.0/js/bootstrap.min.js"></script>
    <script src="http://168.202.48.143:8080/agrovoc/js/jquery.agrovoc.js"></script>
</head>
<body>
<div class="container">

    <form action="agrovoc.jsp" method="get" class="form-horizontal">
        <fieldset>
            <legend>Agrovoc example</legend>
            <div class="control-group">
                <div class="control-label">Rendering component</div>

                <div class="controls">
                    <ul class="inline selected-agrovoc-terms"
                        data-provide="agrovoc"
                        data-codes="${fn:join(paramValues['terms'],', ')}"
                        data-url="http://168.202.48.143:8080/agrovoc"></ul>
                </div>
            </div>
            <hr>
            <div class="control-group">
                <label class="control-label" for="terms">Tagging component</label>

                <div class="controls">
                    <input id="terms" name="terms" type="text" autocomplete="off"
                           placeholder="Type something..."
                           data-provide="agrovoc"
                           data-codes="${fn:join(paramValues['terms'],', ')}"
                           data-url="http://168.202.48.143:8080/agrovoc">

                    <input type="submit" value="Submit" class="btn">
                </div>
            </div>
        </fieldset>
    </form>
</div>
</body>
</html>
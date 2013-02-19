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
    <style>
        li.agrovoc-term {
            -webkit-border-radius: 9px;
            -moz-border-radius: 9px;
            border-radius: 9px;
            margin: 2px 4px;
            height: 20px;
            line-height: 20px;
            color: white;
            font-size: 12px;
            font-weight: bold;
        }

        ul.selected-agrovoc-terms li.agrovoc-term {
            background-color: #3a87ad;
        }

        ul.suggested-agrovoc-terms li.agrovoc-term {
            background-color: lightgray;
        }

        li.agrovoc-term span {
            padding: 0 4px;
            white-space: nowrap;
        }
    </style>
</head>
<body>
<div class="container">
    <h1>Agrovoc service test</h1>

    <form action="agrovoc.jsp" method="get">
        <label for="terms"> Query: </label>

        <input id="terms" name="terms" type="text" autocomplete="off"
               data-provide="agrovoc"
               data-url="http://168.202.48.143:8080/agrovoc"
               data-codes="${fn:join(paramValues['terms'],', ')}">
        <input type="submit" value="Submit">

    </form>
</div>
<script type="text/javascript">
    $(function () {
        $('#id').focus();
    })
</script>

</body>
</html>
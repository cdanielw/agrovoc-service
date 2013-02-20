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
    <script src="http://168.202.48.143:8080/agrovoc/js/jquery.agrovoc2.js"></script>
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

        input#terms {
            margin-bottom: 9px;
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

    <form action="agrovoc2.jsp" method="get" class="form-horizontal">
        <fieldset>
            <legend>Agrovoc example</legend>
            <div class="control-group">
                <div class="control-label">Submitted terms</div>

                <div class="controls">
                    <ul class="inline selected-agrovoc-terms"
                        data-provide="agrovoc"
                        data-codes="${fn:join(paramValues['terms'],', ')}"
                        data-url="http://168.202.48.143:8080/agrovoc"></ul>
                </div>
            </div>
            <div class="control-group">
                <label class="control-label" for="terms">Terms</label>

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
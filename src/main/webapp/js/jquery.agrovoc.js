!function ($) {
    /* AGROVOC PUBLIC CLASS DEFINITION
     * ================================= */

    var Agrovoc = function (element, options) {
        this.$element = $(element);
        this.inputName = this.$element.attr('name');
        this.$element.removeAttr('name');
        this.$termsElement = this.insertTermsElement();
        this.options = $.extend({}, $.fn.agrovoc.defaults, options);
        this.url = this.options.url;
        this.$element.typeahead($.extend({
            agrovoc: this,
            source: this.source,
            highlighter: this.highlighter,
            matcher: this.matcher,
            sorter: this.sorter,
            updater: this.updater
        }, $.fn.agrovoc.defaults, options));
        this.typeahead = this.$element.data('typeahead');
        this.terms = [];
        this.termsByLabel = {};
        this.loadTerms();
    };

    Agrovoc.prototype = {
        constructor: Agrovoc,
        loadTerms: function () {
            var agrovoc = this;
            var codes = this.options.codes ? $.parseJSON('[' + this.options.codes + ']') : [];
            $.getJSON(this.options.url + '/term', { code: codes }, function (data) {
                agrovoc.addTerms(data)
            })
        },
        addTerm: function (term) {
            this.terms.push(term);
            this.$termsElement.append('<li>' + term.label + '</li>');
            this.$element.after('<input type="hidden" name="' + this.inputName + '" value="' + term.code + '"/>');
            this.terms.push(term);
        },
        addTerms: function (terms) {
            var agrovoc = this;
            $.each(terms, function (i, term) {
                agrovoc.addTerm(term);
            });
        },
        insertTermsElement: function () {
            return $('<ul class="agrovoc-terms"></ul>').insertAfter(this.$element);
        },
        findAllTerms: function (query, startsWith) {
            return $.getJSON(this.options.url + '/term/find', {
                q: query,
                language: this.options.language,
                max: this.options.items,
                startsWith: startsWith });
        },
        findTermByLabel: function (label) {
            return $.getJSON(this.options.url + '/term/label/' + label, { language: this.options.language });
        },
        source: function (query, process) {
            var agrovoc = this.options.agrovoc;
            $.when(
                    agrovoc.findTermByLabel(query),
                    agrovoc.findAllTerms(query, true),
                    agrovoc.findAllTerms(query, false)
                ).done(function (exact, startsWith, wordStartsWith) {
                    agrovoc.termsByLabel = {};
                    var terms = $.map([exact[0], startsWith[0], wordStartsWith[0]], function (t) {
                        return t;
                    });

                    var labels = [];
                    $.each(terms, function (i, term) {
                        var label = term.label;
                        if (label !== undefined && $.inArray(label, labels) === -1) {
                            labels.push(label);
                            agrovoc.termsByLabel[label] = term;
                        }
                    });
                    return process(labels);
                }).fail(function () {
                    alert('failed')
                });
        },
        highlighter: function (label) {
            return label;
        },
        matcher: function (label) {
            return true;
        },
        sorter: function (labels) {
            return labels;
        },
        updater: function (label) {
            this.options.agrovoc.addTerm(this.options.agrovoc.termsByLabel[label]);
            return '';
        }
    };


    /* AGROVOC PLUGIN DEFINITION
     * =========================== */

    var old = $.fn.agrovoc;

    $.fn.agrovoc = function (option) {
        return this.each(function () {
            var $this = $(this),
                data = $this.data('agrovoc'),
                options = typeof option == 'object' && option;
            if (!data) $this.data('agrovoc', (data = new Agrovoc(this, options)));
            if (typeof option == 'string') data[option]()
        })
    };

    $.fn.agrovoc.defaults = {
        url: 'http://foris.fao.org/agrovoc',
        language: 'EN'
    };

    $.fn.agrovoc.Constructor = Agrovoc;


    /* AGROVOC NO CONFLICT
     * =================== */

    $.fn.agrovoc.noConflict = function () {
        $.fn.agrovoc = old;
        return this;
    };


    /* AGROVOC DATA-API
     * ================== */

    $(window).on('load', function (e) {
        $('[data-provide="agrovoc"]').each(function () {
            var $this = $(this);
            if ($this.data('agrovoc')) return;
            $this.agrovoc($this.data())
        });
    });

}
    (window.jQuery);
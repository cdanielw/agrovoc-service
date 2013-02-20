!function ($) {

    var Agrovoc = function (element, options) {
        this.options = $.extend({}, $.fn.agrovoc.defaults, options);
        this.termAddedListeners = [];
        this.terms = [];
        this.termsByCode = {};
        this.$element = $(element);
        var component;
        if (this.$element.is('input'))
            component = new Selector(this.$element, this);
        else
            component = new Displayer(this.$element, this);
        this.termTemplate = component.getTermTemplate();
        this.$selectedTerms = component.$selectedTerms;
        this.loadTerms();
    };

    Agrovoc.prototype = {
        constructor: Agrovoc,

        loadTerms: function () {
            if (!this.options.codes) return;
            var codes = this.options.codes ? $.parseJSON('[' + this.options.codes + ']') : [];
            var that = this;
            this.jsonpFromRelativeUrl('/term', { code: codes }).done(function (terms) {
                that.addTerms(terms, that.$selectedTerms)
            });
        },

        addTerms: function (terms, $terms) {
            this.terms = terms;
            this.renderTerms(terms, $terms);
            var that = this;
            $.each(terms, function (i, term) {
                that.termsByCode[term.code] = term;
                that.notifyTermAddedListeners(term, $terms);
            });
        },

        addTerm: function (term) {
            if (this.isTermAlreadyAdded(term)) return;
            this.terms.push(term);
            this.termsByCode[term.code] = term;
            this.renderTerm(term, this.$selectedTerms);
            this.notifyTermAddedListeners(term);
        },

        isTermAlreadyAdded: function (term) {
            var termAdded = false;
            $.each(this.terms, function () {
                if (this.code == term.code) {
                    termAdded = true;
                    return false;
                }
            });
            return termAdded;
        },
        removeTerm: function (term) {
            this.terms = $.grep(this.terms, function (t) {
                return t.code != term.code;
            });
            this.termsByCode[term.code] = null;
            this.$selectedTerms.children().each(function () {
                var $item = $(this);
                if ($item.data('code') == term.code) {
                    $item.remove();
                    return false;
                }
            });
        },

        notifyTermAddedListeners: function (term) {
            var that = this;
            $.each(this.termAddedListeners, function (i, termListener) {
                termListener(term);
            });
        },

        jsonpFromRelativeUrl: function (relativeUrl, data) {
            return this.jsonpFromAbsoluteUrl(this.options.url + relativeUrl, data);
        },

        jsonpFromAbsoluteUrl: function (absoluteUrl, data) {
            var dataWithLanguage = $.extend({ language: this.options.language }, data);
            var url = this.addJsonpCallbackParam(absoluteUrl);
            return $.getJSON(url, dataWithLanguage);
        },

        addJsonpCallbackParam: function (url) {
            return url + (url.indexOf('?') < 0 ? '?' : '&') + 'callback=?';
        },

        renderTerms: function (terms, $terms) {
            var that = this;
            $.each(terms, function (i, term) {
                that.renderTerm(term, $terms)
            })
        },

        renderTerm: function (term, $terms) {
            var $selectedTerm = $(this.termTemplate);
            $selectedTerm.attr('data-code', term.code);
            $selectedTerm.find('.agrovoc-term-label').text(term.label);
            var $itemToInsertBefore = this.findTermListItemToInsertBefore(term, $terms);
            if ($itemToInsertBefore)
                $selectedTerm.insertBefore($itemToInsertBefore);
            else
                $terms.append($selectedTerm);
        },

        findTermListItemToInsertBefore: function (term, $parentElement) {
            var $itemToInsertBefore = null;
            $parentElement.children().each(function (i, item) {
                var $item = $(item);
                var label = $item.find('span').text();
                if (label.toLowerCase() > term.label.toLowerCase()) {
                    $itemToInsertBefore = $item;
                    return false;
                }
            });
            return $itemToInsertBefore;
        }
    };

    var Selector = function ($element, agrovoc) {
        this.agrovoc = agrovoc;
        this.$suggestedTerms = this.initSuggestedTermsSelector();
        this.$selectedTerms = this.initSelectedTermsSelector(this.$suggestedTerms);
        this.$hiddenInputs = this.initInputSelector();
        this.termsByLabel = {};
        this.createTypeahead($element);
        this.listen();
    };

    Selector.prototype = {
        constructor: Selector,

        listen: function () {
            var that = this;
            this.agrovoc.termAddedListeners.push(function (term) {
                that.termAdded(term)
            });
            this.$selectedTerms.on('click', '.agrovoc-term', function (event) {
                event.preventDefault();
                var code = $(this).data('code');
                var term = that.agrovoc.termsByCode[code];
                that.agrovoc.renderTerm(term, that.$suggestedTerms);
            });
            this.$suggestedTerms.on('click', '.agrovoc-term', function (event) {
                event.preventDefault();
                var code = $(this).data('code');
                var term = that.agrovoc.termsByCode[code];
                that.agrovoc.addTerm(term);
            });
        },

        getTermTemplate: function () {
            var template = this.agrovoc.options.termTemplate;
            if (!template)
                return '' +
                    '<li class="agrovoc-term" data-code="">' +
                    '  <i class="icon-minus-sign icon-white"></i>' +
                    '  <span class="agrovoc-term-label"></span>' +
                    '</li>';
            return template;
        },

        initSelectedTermsSelector: function ($suggestedTerms) {
            var $selectedTerms = $(this.agrovoc.options.$selectedTerms);
            if ($selectedTerms.length == 0) {
                $selectedTerms = $('<ul class="inline selected-agrovoc-terms"></ul>')
                    .insertAfter($suggestedTerms);
            }
            $selectedTerms
                .attr('unselectable', 'on')
                .css('user-select', 'none')
                .css('cursor', 'pointer');
            return $selectedTerms
        },

        initSuggestedTermsSelector: function () {
            var $suggestedTerms = $(this.agrovoc.options.$suggestedTerms);
            if ($suggestedTerms.length == 0) {
                $suggestedTerms = $('<ul class="inline suggested-agrovoc-terms"></ul>')
                    .insertAfter(this.agrovoc.$element);
            }
            $suggestedTerms
                .attr('unselectable', 'on')
                .css('user-select', 'none')
                .css('cursor', 'pointer');
            return $suggestedTerms
        },


        initInputSelector: function () {
            return $('<div class="agrovoc-hidden"></div>').insertAfter(this.$element);
        },

        createTypeahead: function ($element) {
            var that = this;
            $element.typeahead($.extend({
                source: function (query, process) {
                    that.findAll(query, process);
                },
                highlighter: function (label) {
                    return that.highlighter(label)
                },
                matcher: function (label) {
                    return true;
                },
                sorter: function (labels) {
                    return labels;
                },
                updater: function (label) {
                    return that.updater(label);
                }
            }, $.fn.agrovoc.defaults, this.agrovoc.options));
        },

        findAll: function (query, process) {
            query = $.trim(query);
            if (query.length == 0) return false;
            this.$suggestedTerms.empty();
            this.termsByLabel = {};
            var that = this;
            $.when(
                    this.executeFindExactMatch(query),
                    this.executeFindAllThatStartsWith(query, true),
                    this.executeFindAllWhereWordStartsWith(query, false)
                ).done(function (exact, startsWith, wordStartsWith) {
                    var terms = that.flatten([exact[0], startsWith[0], wordStartsWith[0]]);
                    var labels = that.unique(
                        $.map(terms, function (term) {
                            that.termsByLabel[term.label] = term;
                            return term.label
                        })
                    );
                    return process(labels);
                });
        },

        executeFindExactMatch: function (query) {
            return this.agrovoc.jsonpFromRelativeUrl('/term/label/' + query);
        },

        executeFindAllThatStartsWith: function (query) {
            return this.agrovoc.jsonpFromRelativeUrl('/term/find', {
                q: query,
                max: this.agrovoc.options.items,
                startsWith: true });
        },

        executeFindAllWhereWordStartsWith: function (query) {
            return this.agrovoc.jsonpFromRelativeUrl('/term/find', {
                q: query,
                max: this.agrovoc.options.items});
        },

        highlighter: function (label) {
            var term = this.termsByLabel[label];
            return !this.isTermDescriptor(term) ?
                '<span class="muted">' + label + '</span>' : label;
        },

        updater: function (label) {
            var term = this.termsByLabel[label];
            if (this.isTermDescriptor(term))
                this.agrovoc.addTerm(term);

            var that = this;
            this.agrovoc.jsonpFromAbsoluteUrl(term.links.broader).done(function (suggestedTerms) {
                that.suggestTerms(suggestedTerms);
            });

            return '';
        },

        suggestTerms: function (terms) {
            this.agrovoc.renderTerms(terms, this.$suggestedTerms);
        },

        termAdded: function (term) {
            this.$hiddenInputs.append('<input type="hidden" name="' + this.inputName + '" value="' + term.code + '"/>');
        },

        isTermDescriptor: function (term) {
            return term.status == 20;
        },

        flatten: function (array) {
            return $.map(array, function (item) {
                return item;
            });
        },

        unique: function (array) {
            var unique = [];
            $.each(array, function (i, item) {
                if ($.inArray(item, unique) === -1)
                    unique.push(item);
            });
            return unique
        }

    };

    var Displayer = function ($element, agrovoc) {
        this.agrovoc = agrovoc;
        this.$selectedTerms = this.initSelectedTermsSelector();
    };

    Displayer.prototype = {
        constructor: Displayer,

        getTermTemplate: function () {
            var template = this.agrovoc.options.termTemplate;
            if (!template)
                return '' +
                    '<li class="agrovoc-term" data-code="">' +
                    '  <span class="agrovoc-term-label"></span>' +
                    '</li>';
            return template;
        },

        initSelectedTermsSelector: function () {
            return this.agrovoc.$element;
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
            if (!data) {
                $this.data('agrovoc', (data = new Agrovoc(this, options)));
            }
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

}(window.jQuery);
!function ($) {

    var Agrovoc = function (element, options) {
        this.options = $.extend({}, $.fn.agrovoc.defaults, options);
        this.termsLoadedListeners = [];
        this.termAddedListeners = [];
        this.termRemovedListeners = [];
        this.terms = [];
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
            if (!this.options.codes || this.options.codes.length == 0) return;
            var codes = this.options.codes ? $.parseJSON('[' + this.options.codes + ']') : [];
            var that = this;
            this.jsonpFromRelativeUrl('/term', { code: codes }).done(function (json) {
                that.addTerms(json.results, that.$selectedTerms);
                that.notifyListeners(that.terms, that.termsLoadedListeners);
                that.$element.trigger($.Event('loaded'), [that.terms]);
            });
        },

        addTerms: function (terms, $terms) {
            this.terms = terms;
            this.renderTerms(terms, $terms);
            var that = this;
        },

        addTerm: function (term) {
            if (this.isTermAlreadyAdded(term)) return;
            this.terms.push(term);
            this.renderTerm(term, this.$selectedTerms, this.termTemplate);
            this.notifyListeners(term, this.termAddedListeners);
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
            this.removeRenderedTerm(term, this.$selectedTerms);
            this.notifyListeners(term, this.termRemovedListeners);
        },

        removeRenderedTerm: function (term, $terms) {
            this.removeChild($terms, function ($item) {
                return $item.data('code') == term.code;
            });
        },

        notifyListeners: function (object, listeners) {
            $.each(listeners, function (i, listener) {
                listener(object);
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

        renderTerms: function (terms, $terms, template) {
            if (!template)
                template = this.termTemplate;
            var that = this;
            $.each(terms, function (i, term) {
                that.renderTerm(term, $terms, template)
            })
        },

        renderTerm: function (term, $terms, template) {
            var $term = $(template);
            $term.data('term', term);
            $term.attr('data-code', term.code);
            $term.find('.agrovoc-term-label').text(term.label);
            var $itemToInsertBefore = this.findTermListItemToInsertBefore(term, $terms);

            $term.css('opacity', 0.0);
            if ($itemToInsertBefore)
                $term.insertBefore($itemToInsertBefore);
            else
                $terms.append($term);
            $term.animate({ opacity: 1.0, duration: 50 });
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
        },

        removeChild: function ($items, callback) {
            $items.children().each(function () {
                var $item = $(this);
                if (callback($item)) {
                    $item.remove();
                    return false;
                }
            });
        }
    };

    var Selector = function ($element, agrovoc) {
        this.agrovoc = agrovoc;
        this.initSuggestOption();
        this.disableAutocomplete($element);
        this.$suggestedTerms = this.initSuggestedTerms();
        this.$selectedTerms = this.initSelectedTerms(this.$suggestedTerms);
        this.$hiddenInputs = this.initHiddenInputs();
        this.inputName = this.initInputName();
        this.termsByLabel = {};
        this.relationshipTypes = agrovoc.options.suggest;
        this.createTypeahead($element);
        this.listen();
    };

    Selector.prototype = {
        constructor: Selector,

        initSuggestOption: function () {
            var options = this.agrovoc.options;
            if ($.type(options.suggest) !== "string") return;
            options.suggest = $.map(options.suggest.split(','), function (relationshipType) {
                return $.trim(relationshipType);
            });
        },

        disableAutocomplete: function ($element) {
            $element.attr('autocomplete', 'off');
        },

        listen: function () {
            var that = this;
            this.agrovoc.termsLoadedListeners.push(function (terms) {
                $.each(terms, function (term) {
                    that.termAdded(term)
                })
            });
            this.agrovoc.termAddedListeners.push(function (term) {
                that.termAdded(term)
            });
            this.agrovoc.termRemovedListeners.push(function (term) {
                that.termRemoved(term)
            });
            this.$selectedTerms.on('click', '.agrovoc-term', function (event) {
                event.preventDefault();
                that.agrovoc.$element.focus();
                var term = $(this).data('term');
                that.agrovoc.removeTerm(term);
                that.agrovoc.renderTerm(term, that.$suggestedTerms, that.agrovoc.termTemplate);
                that.agrovoc.$element.trigger($.Event('removed'), [term]);
            });
            this.$suggestedTerms.on('click', '.agrovoc-term', function (event) {
                event.preventDefault();
                that.agrovoc.$element.focus();
                var term = $(this).data('term');
                that.agrovoc.addTerm(term);
                that.agrovoc.removeRenderedTerm(term, that.$suggestedTerms);
                that.agrovoc.$element.trigger($.Event('selected'), [term]);
            });
        },

        getTermTemplate: function () {
            return '' +
                '<li class="agrovoc-term" data-code="">' +
                '  <i class="icon-minus-sign icon-white"></i>' +
                '  <span class="agrovoc-term-label"></span>' +
                '</li>';
        },

        getSuggestedTermTemplate: function () {
            var template = this.agrovoc.options.suggestedTermTemplate;
            if (!template)
                return '' +
                    '<li class="agrovoc-term" data-code="">' +
                    '  <i class="icon-plus-sign icon-white"></i>' +
                    '  <span class="agrovoc-term-label"></span>' +
                    '</li>';
            return template;
        },

        initSelectedTerms: function ($suggestedTerms) {
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

        initSuggestedTerms: function () {
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


        initHiddenInputs: function () {
            return $('<div></div>').insertAfter(this.agrovoc.$element);
        },

        initInputName: function () {
            var inputName = this.agrovoc.$element.attr('name');
            this.agrovoc.$element.removeAttr('name');
            return  inputName;
        },

        createTypeahead: function ($element) {
            var that = this;
            var typeaheadOptions = {
                source: function (query, process) {
                    that.findAll(query, process);
                },
                items: that.agrovoc.options.hits,
                matcher: function (label) {
                    return true;
                },
                sorter: function (labels) {
                    return labels;
                },
                updater: function (label) {
                    return that.updater(label);
                },
                highlighter: function (label) {
                    return that.highlighter(label)
                }

            };
            $element.typeahead(typeaheadOptions);
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
                    var terms = that.flatten($.map([exact[0], startsWith[0], wordStartsWith[0]], function (resultSet) {
                        return resultSet.results
                    }));
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
            return this.agrovoc.jsonpFromRelativeUrl('/term/find', {
                q: query,
                hits: 1,
                match: 'exact',
                suggestions: this.agrovoc.options.suggestions,
                relationshipType: this.relationshipTypes});
        },

        executeFindAllThatStartsWith: function (query) {
            return this.agrovoc.jsonpFromRelativeUrl('/term/find', {
                q: query,
                hits: this.agrovoc.options.hits,
                match: 'startsWith',
                suggestions: this.agrovoc.options.suggestions,
                relationshipType: this.relationshipTypes });
        },

        executeFindAllWhereWordStartsWith: function (query) {
            return this.agrovoc.jsonpFromRelativeUrl('/term/find', {
                q: query,
                hits: this.agrovoc.options.hits,
                match: 'freeText',
                suggestions: this.agrovoc.options.suggestions,
                relationshipType: this.relationshipTypes});
        },

        highlighter: function (label) {
            var term = this.termsByLabel[label];
            return !term.preferred ?
                '<span class="muted">' + label + '</span>' : label;
        },

        updater: function (label) {
            var term = this.termsByLabel[label];
            if (term.preferred) {
                this.agrovoc.addTerm(term);
                this.agrovoc.$element.trigger($.Event('selected'), [term]);
            }

            var that = this;
            if (term.relationships)
                this.agrovoc.jsonpFromAbsoluteUrl(term.relationships).done(function (json) {
                    that.suggestTerms(json.results);
                });
            return '';
        },

        suggestTerms: function (terms) {
            var that = this;
            var termsToSuggest = $.grep(terms, function (term) {
                return !that.agrovoc.isTermAlreadyAdded(term)
            });
            this.agrovoc.renderTerms(termsToSuggest, this.$suggestedTerms, this.getSuggestedTermTemplate());
        },

        termAdded: function (term) {
            this.$hiddenInputs.append('<input type="hidden" name="' + this.inputName + '" value="' + term.code + '"/>');
        },

        termRemoved: function (term) {
            this.agrovoc.removeChild(this.$hiddenInputs, function ($item) {
                return parseInt($item.val()) == term.code;
            });
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
        this.$selectedTerms = this.initSelectedTerms();
        this.addElementClasses($element);
    };

    Displayer.prototype = {
        constructor: Displayer,

        addElementClasses: function ($element) {
            $element.addClass('inline');
        },

        getTermTemplate: function () {
            return '' +
                '<li class="agrovoc-term" data-code="">' +
                '  <span class="agrovoc-term-label"></span>' +
                '</li>';
        },

        initSelectedTerms: function () {
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
        hits: 8,
        suggestions: 8,
        suggest: ['synonym', 'broader'],
        language: 'EN',
        url: 'http://foris.fao.org/agrovoc'
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
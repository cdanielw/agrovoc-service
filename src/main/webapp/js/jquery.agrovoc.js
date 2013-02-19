!function ($) {
    /* AGROVOC PUBLIC CLASS DEFINITION
     * ================================= */

    var Agrovoc = function (element, options) {
        this.$element = $(element);
        this.inputName = this.$element.attr('name');
        this.$element.removeAttr('name');
        this.$selectedTermsElement = this.insertSelectedTermsElement();
        this.$suggestedTermsElement = this.insertSuggestedTermsElement();
        this.$inputsElement = this.insertInputsElement();
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
        removeTerm: function (term) {
            $.grep(this.terms, function () {
                return this.code != term.code;
            });
            this.$selectedTermsElement.children().each(function () {
                var $item = $(this);
                if ($item.data('code') == term.code) {
                    $item.remove();
                    return false;
                }
            });
            this.$inputsElement.children('input').each(function () {
                var $input = $(this);
                if ($input.val() == term.code) {
                    $input.remove();
                    return false;
                }
            });
        },
        selectTerm: function (term) {
            if (this.isTermAlreadySelected(term)) return false;
            var $item = $('<li class="agrovoc-term" data-code="' + term.code + '">'
                + '<i class="icon-remove-sign icon-white"></i><span>' + term.label
                + '</span></li>');
            this.appendTermListItem(term, this.$selectedTermsElement, $item);

            var agrovoc = this;
            $item.click(function (event) {
                event.preventDefault();
                agrovoc.removeTerm(term);
                return false;
            });
            this.$inputsElement.append('<input type="hidden" name="' + this.inputName + '" value="' + term.code + '"/>');
            this.terms.push(term);
        },
        addSuggestedTerm: function (term) {
            var $item = $('<li class="agrovoc-term" data-code="' + term.code + '">'
                + '<i class="icon-plus-sign icon-white"></i><span>' + term.label
                + '</span></li>');

            var agrovoc = this;
            $item.click(function (event) {
                event.preventDefault();
                $item.remove();
                agrovoc.selectTerm(term);
                return false;
            });
            this.appendTermListItem(term, this.$suggestedTermsElement, $item);
        },
        isTermAlreadySelected: function (term) {
            var termAdded = false;
            $.each(this.terms, function () {
                if (this.code == term.code) {
                    termAdded = true;
                    return false;
                }
            });
            return termAdded;
        },
        appendTermListItem: function (term, $parentElement, $item) {
            $item.css('cursor', 'pointer');
            var $itemToInsertBefore = this.findTermListItemToInsertBefore(term, $parentElement);
            if ($itemToInsertBefore != null) {
                $($item)
                    .hide()
                    .css('opacity', 0.0)
                    .insertBefore($itemToInsertBefore)
                    .slideDown('fast')
                    .animate({ opacity: 1.0, duration: 'fast' })
            } else {
                $($item)
                    .hide()
                    .css('opacity', 0.0)
                    .appendTo($parentElement)
                    .slideDown('fast')
                    .animate({ opacity: 1.0, duration: 'fast' });
            }
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
        addTerms: function (terms) {
            var agrovoc = this;
            $.each(terms, function (i, term) {
                agrovoc.selectTerm(term);
            });
        },
        insertSelectedTermsElement: function () {
            return $('<ul class="inline selected-agrovoc-terms"></ul>').insertAfter(this.$element);
        },
        insertSuggestedTermsElement: function () {
            return $('<ul class="inline suggested-agrovoc-terms"></ul>').insertAfter(this.$element);
        },
        insertInputsElement: function () {
            return $('<div class="agrovoc-hidden"></ul>').insertAfter(this.$element);
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
        isTermDescriptor: function (term) {
            return term.status == 20;
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
            var agrovoc = this.options.agrovoc;
            var term = agrovoc.termsByLabel[label];
            if (!agrovoc.isTermDescriptor(term))
                return '<span class="muted">' + label + '</span>';
            return label;
        },
        matcher: function (label) {
            return true;
        },
        sorter: function (labels) {
            return labels;
        },
        updater: function (label) {
            var agrovoc = this.options.agrovoc;
            var term = agrovoc.termsByLabel[label];
            if (agrovoc.isTermDescriptor(term))
                agrovoc.selectTerm(term); // TODO: Handle case where non-term descriptor is selected
            var url = term.links.broader;
            $.getJSON(url).done(function (broaderTerms) {
                agrovoc.$suggestedTermsElement.remove();
                agrovoc.$suggestedTermsElement = agrovoc.insertSuggestedTermsElement();
                $.each(broaderTerms, function (i, t) {
                    agrovoc.addSuggestedTerm(t);
                });
            });

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

}(window.jQuery);
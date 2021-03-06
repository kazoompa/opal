/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

ace.define('ace/keyboard/emacs', ['require', 'exports', 'module' , 'ace/lib/dom', 'ace/keyboard/hash_handler', 'ace/lib/keys'], function (require, exports, module) {


    var dom = require("../lib/dom");

    var screenToTextBlockCoordinates = function (x, y) {
        var canvasPos = this.scroller.getBoundingClientRect();

        var col = Math.floor(
            (x + this.scrollLeft - canvasPos.left - this.$padding) / this.characterWidth
        );
        var row = Math.floor(
            (y + this.scrollTop - canvasPos.top) / this.lineHeight
        );

        return this.session.screenToDocumentPosition(row, col);
    };

    var HashHandler = require("./hash_handler").HashHandler;
    exports.handler = new HashHandler();

    var initialized = false;
    var $formerLongWords;
    var $formerLineStart;

    exports.handler.attach = function (editor) {
        if (!initialized) {
            initialized = true;

            dom.importCssString('\
            .emacs-mode .ace_cursor{\
                border: 2px rgba(50,250,50,0.8) solid!important;\
                -moz-box-sizing: border-box!important;\
                -webkit-box-sizing: border-box!important;\
                box-sizing: border-box!important;\
                background-color: rgba(0,250,0,0.9);\
                opacity: 0.5;\
            }\
            .emacs-mode .ace_cursor.ace_hidden{\
                opacity: 1;\
                background-color: transparent;\
            }\
            .emacs-mode .ace_overwrite-cursors .ace_cursor {\
                opacity: 1;\
                background-color: transparent;\
                border-width: 0 0 2px 2px !important;\
            }\
            .emacs-mode .ace_text-layer {\
                z-index: 4\
            }\
            .emacs-mode .ace_cursor-layer {\
                z-index: 2\
            }', 'emacsMode'
            );
        }
        $formerLongWords = editor.session.$selectLongWords;
        editor.session.$selectLongWords = true;
        $formerLineStart = editor.session.$useEmacsStyleLineStart;
        editor.session.$useEmacsStyleLineStart = true;

        editor.session.$emacsMark = null;

        exports.markMode = function () {
            return editor.session.$emacsMark;
        }

        exports.setMarkMode = function (p) {
            editor.session.$emacsMark = p;
        }

        editor.on("click", $resetMarkMode);

        editor.on("changeSession", $kbSessionChange);

        editor.renderer.screenToTextCoordinates = screenToTextBlockCoordinates;

        editor.setStyle("emacs-mode");

    };

    exports.handler.detach = function (editor) {

        delete editor.renderer.screenToTextCoordinates;

        editor.session.$selectLongWords = $formerLongWords;
        editor.session.$useEmacsStyleLineStart = $formerLineStart;


        editor.removeEventListener("click", $resetMarkMode);
        editor.removeEventListener("changeSession", $kbSessionChange);

        editor.unsetStyle("emacs-mode");
    };

    var $kbSessionChange = function (e) {
        if (e.oldSession) {
            e.oldSession.$selectLongWords = $formerLongWords;
            e.oldSession.$useEmacsStyleLineStart = $formerLineStart;
        }

        $formerLongWords = e.session.$selectLongWords;
        e.session.$selectLongWords = true;
        $formerLineStart = e.session.$useEmacsStyleLineStart;
        e.session.$useEmacsStyleLineStart = true;

        if (!e.session.hasOwnProperty('$emacsMark'))
            e.session.$emacsMark = null;
    }

    var $resetMarkMode = function (e) {
        e.editor.session.$emacsMark = null;
    }

    var keys = require("../lib/keys").KEY_MODS;
    var eMods = {
        C: "ctrl", S: "shift", M: "alt"
    };
    ["S-C-M", "S-C", "S-M", "C-M", "S", "C", "M"].forEach(function (c) {
        var hashId = 0;
        c.split("-").forEach(function (c) {
            hashId = hashId | keys[eMods[c]];
        });
        eMods[hashId] = c.toLowerCase() + "-";
    });

    exports.handler.bindKey = function (key, command) {
        if (!key)
            return;

        var ckb = this.commmandKeyBinding;
        key.split("|").forEach(function (keyPart) {
            keyPart = keyPart.toLowerCase();
            ckb[keyPart] = command;
            keyPart = keyPart.split(" ")[0];
            if (!ckb[keyPart])
                ckb[keyPart] = "null";
        }, this);
    };


    exports.handler.handleKeyboard = function (data, hashId, key, keyCode) {
        if (hashId == -1) {
            exports.setMarkMode(null);
            if (data.count) {
                var str = Array(data.count + 1).join(key);
                data.count = null;
                return {command: "insertstring", args: str};
            }
        }

        if (key == "\x00")
            return;

        var modifier = eMods[hashId];
        if (modifier == "c-" || data.universalArgument) {
            var count = parseInt(key[key.length - 1]);
            if (count) {
                data.count = count;
                return {command: "null"};
            }
        }
        data.universalArgument = false;

        if (modifier)
            key = modifier + key;

        if (data.keyChain)
            key = data.keyChain += " " + key;

        var command = this.commmandKeyBinding[key];
        data.keyChain = command == "null" ? key : "";

        if (!command)
            return;

        if (command == "null")
            return {command: "null"};

        if (command == "universalArgument") {
            data.universalArgument = true;
            return {command: "null"};
        }

        if (typeof command != "string") {
            var args = command.args;
            command = command.command;
            if (command == "goorselect") {
                command = args[0];
                if (exports.markMode()) {
                    command = args[1];
                }
                args = null;
            }
        }

        if (typeof command == "string") {
            if (command == "insertstring" ||
                command == "splitline" ||
                command == "togglecomment") {
                exports.setMarkMode(null);
            }
            command = this.commands[command] || data.editor.commands.commands[command];
        }

        if (!command.readonly && !command.isYank)
            data.lastCommand = null;

        if (data.count) {
            var count = data.count;
            data.count = 0;
            return {
                args: args,
                command: {
                    exec: function (editor, args) {
                        for (var i = 0; i < count; i++)
                            command.exec(editor, args);
                    }
                }
            };
        }

        return {command: command, args: args};
    };

    exports.emacsKeys = {
        "Up|C-p": {command: "goorselect", args: ["golineup", "selectup"]},
        "Down|C-n": {command: "goorselect", args: ["golinedown", "selectdown"]},
        "Left|C-b": {command: "goorselect", args: ["gotoleft", "selectleft"]},
        "Right|C-f": {command: "goorselect", args: ["gotoright", "selectright"]},
        "C-Left|M-b": {command: "goorselect", args: ["gotowordleft", "selectwordleft"]},
        "C-Right|M-f": {command: "goorselect", args: ["gotowordright", "selectwordright"]},
        "Home|C-a": {command: "goorselect", args: ["gotolinestart", "selecttolinestart"]},
        "End|C-e": {command: "goorselect", args: ["gotolineend", "selecttolineend"]},
        "C-Home|S-M-,": {command: "goorselect", args: ["gotostart", "selecttostart"]},
        "C-End|S-M-.": {command: "goorselect", args: ["gotoend", "selecttoend"]},
        "S-Up|S-C-p": "selectup",
        "S-Down|S-C-n": "selectdown",
        "S-Left|S-C-b": "selectleft",
        "S-Right|S-C-f": "selectright",
        "S-C-Left|S-M-b": "selectwordleft",
        "S-C-Right|S-M-f": "selectwordright",
        "S-Home|S-C-a": "selecttolinestart",
        "S-End|S-C-e": "selecttolineend",
        "S-C-Home": "selecttostart",
        "S-C-End": "selecttoend",

        "C-l": "recenterTopBottom",
        "M-s": "centerselection",
        "M-g": "gotoline",
        "C-x C-p": "selectall",
        "C-Down": {command: "goorselect", args: ["gotopagedown", "selectpagedown"]},
        "C-Up": {command: "goorselect", args: ["gotopageup", "selectpageup"]},
        "PageDown|C-v": {command: "goorselect", args: ["gotopagedown", "selectpagedown"]},
        "PageUp|M-v": {command: "goorselect", args: ["gotopageup", "selectpageup"]},
        "S-C-Down": "selectpagedown",
        "S-C-Up": "selectpageup",
        "C-s": "findnext",
        "C-r": "findprevious",
        "M-C-s": "findnext",
        "M-C-r": "findprevious",
        "S-M-5": "replace",
        "Backspace": "backspace",
        "Delete|C-d": "del",
        "Return|C-m": {command: "insertstring", args: "\n"}, // "newline"
        "C-o": "splitline",

        "M-d|C-Delete": {command: "killWord", args: "right"},
        "C-Backspace|M-Backspace|M-Delete": {command: "killWord", args: "left"},
        "C-k": "killLine",

        "C-y|S-Delete": "yank",
        "M-y": "yankRotate",
        "C-g": "keyboardQuit",

        "C-w": "killRegion",
        "M-w": "killRingSave",
        "C-Space": "setMark",
        "C-x C-x": "exchangePointAndMark",

        "C-t": "transposeletters",
        "M-u": "touppercase",    // Doesn't work
        "M-l": "tolowercase",
        "M-/": "autocomplete",   // Doesn't work
        "C-u": "universalArgument",

        "M-;": "togglecomment",

        "C-/|C-x u|S-C--|C-z": "undo",
        "S-C-/|S-C-x u|C--|S-C-z": "redo", //infinite undo?
        "C-x r": "selectRectangularRegion"
    };


    exports.handler.bindKeys(exports.emacsKeys);

    exports.handler.addCommands({
        recenterTopBottom: function (editor) {
            var renderer = editor.renderer;
            var pos = renderer.$cursorLayer.getPixelPosition();
            var h = renderer.$size.scrollerHeight - renderer.lineHeight;
            var scrollTop = renderer.scrollTop;
            if (Math.abs(pos.top - scrollTop) < 2) {
                scrollTop = pos.top - h;
            } else if (Math.abs(pos.top - scrollTop - h * 0.5) < 2) {
                scrollTop = pos.top;
            } else {
                scrollTop = pos.top - h * 0.5;
            }
            editor.session.setScrollTop(scrollTop);
        },
        selectRectangularRegion: function (editor) {
            editor.multiSelect.toggleBlockSelection();
        },
        setMark: function (editor) {
            var markMode = exports.markMode();

            if (markMode) {

                cp = editor.getCursorPosition();
                if (editor.selection.isEmpty() &&
                    markMode.row == cp.row && markMode.column == cp.column) {
                    exports.setMarkMode(null);
                    return;
                }
            }
            markMode = editor.getCursorPosition();
            exports.setMarkMode(markMode);
            editor.selection.setSelectionAnchor(markMode.row, markMode.column);

        },
        exchangePointAndMark: {
            exec: function (editor) {
                var range = editor.selection.getRange();

                editor.selection.setSelectionRange(range, !editor.selection.isBackwards());
            },
            readonly: true,
            multiselectAction: "forEach"
        },
        killWord: {
            exec: function (editor, dir) {
                editor.clearSelection();
                if (dir == "left")
                    editor.selection.selectWordLeft();
                else
                    editor.selection.selectWordRight();

                var range = editor.getSelectionRange();
                var text = editor.session.getTextRange(range);
                exports.killRing.add(text);

                editor.session.remove(range);
                editor.clearSelection();
            },
            multiselectAction: "forEach"
        },
        killLine: function (editor) {
            exports.setMarkMode(null);
            var pos = editor.getCursorPosition();

            if (pos.column == 0 &&
                editor.session.doc.getLine(pos.row).length == 0) {
                editor.selection.selectLine();
            } else {
                editor.clearSelection();
                editor.selection.selectLineEnd();

            }
            var range = editor.getSelectionRange();
            var text = editor.session.getTextRange(range);
            exports.killRing.add(text);

            editor.session.remove(range);
            editor.clearSelection();
        },
        yank: function (editor) {
            editor.onPaste(exports.killRing.get());
            editor.keyBinding.$data.lastCommand = "yank";
        },
        yankRotate: function (editor) {
            if (editor.keyBinding.$data.lastCommand != "yank")
                return;

            editor.undo();
            editor.onPaste(exports.killRing.rotate());
            editor.keyBinding.$data.lastCommand = "yank";
        },
        killRegion: function (editor) {
            exports.killRing.add(editor.getCopyText());
            editor.commands.byName.cut.exec(editor);
        },
        killRingSave: function (editor) {
            exports.killRing.add(editor.getCopyText());
        }
    });

    var commands = exports.handler.commands;
    commands.yank.isYank = true;
    commands.yankRotate.isYank = true;

    exports.killRing = {
        $data: [],
        add: function (str) {
            str && this.$data.push(str);
            if (this.$data.length > 30)
                this.$data.shift();
        },
        get: function () {
            return this.$data[this.$data.length - 1] || "";
        },
        pop: function () {
            if (this.$data.length > 1)
                this.$data.pop();
            return this.get();
        },
        rotate: function () {
            this.$data.unshift(this.$data.pop());
            return this.get();
        }
    };


});

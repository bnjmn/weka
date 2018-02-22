" Vim syntax file
" Language:	ARFF file
" Maintainer:	fracpete
" Last Change:	2004 Dec 17

" Syntax highlighting for the WEKA ARFF files
" http://www.cs.waikato.ac.nz/ml/weka/

" For version 5.x: Clear all syntax items
" For version 6.x: Quit when a syntax file was already loaded
if version < 600
  syntax clear
elseif exists("b:current_syntax")
  finish
endif

syn case ignore

" keywords
syn keyword arffStatement	relation attribute data end
syn keyword arffTodo contained	TODO
syn keyword arffType	numeric integer real string relational

" comment
syn region arffComment	start="%"  end="$" contains=arffTodo

" numbers
syn match  arffNumber	"-\=\<\d\+\>"
syn match  arffFloat		"-\=\<\d\+\.\d\+\>"
syn match  arffFloat		"-\=\<\d\+\.\d\+[eE]-\=\d\+\>"
syn match  arffSymbol   "@"
syn match  arffSymbol   "{"
syn match  arffSymbol   "}"
syn match  arffSymbol   "?"

" string
syn region  arffString matchgroup=arffString start=+'+ end=+'+ oneline contains=arffStringEscape
syn match   arffStringEscape		contained "\'"

" Define the default highlighting.
" For version 5.7 and earlier: only when not done already
" For version 5.8 and later: only when an item doesn't have highlighting yet
if version >= 508 || !exists("did_arff_syn_inits")
  if version < 508
    let did_arff_syn_inits = 1
    command -nargs=+ HiLink hi link <args>
  else
    command -nargs=+ HiLink hi def link <args>
  endif

  HiLink arffStatement	Statement
  HiLink arffSymbol	Special
  HiLink arffTodo		Todo
  HiLink arffComment		Comment
  HiLink arffFloat		Float
  HiLink arffNumber		Number
  HiLink arffString		String
  HiLink arffStringEscape	Special
  HiLink arffType	Type

  delcommand HiLink
endif

let b:current_syntax = "arff"

" vim: ts=8

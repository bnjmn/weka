(require 'generic)
(define-generic-mode 'arff-file-mode
  (list ?%)
  (list "attribute" "relation" "end" "data")
  '(
    ("\\('.*'\\)" 1 'font-lock-string-face)    
    ("^\\@\\S-*\\s-\\(\\S-*\\)" 1 'font-lock-string-face)    
    ("^\\@.*\\(real\\)" 1 'font-lock-type-face)    
    ("^\\@.*\\(integer\\)" 1 'font-lock-type-face)    
    ("^\\@.*\\(numeric\\)" 1 'font-lock-type-face)    
    ("^\\@.*\\(string\\)" 1 'font-lock-type-face)    
    ("^\\@.*\\(date\\)" 1 'font-lock-type-face)    
    ("^\\@.*\\({.*}\\)" 1 'font-lock-type-face)    
    ("^\\({\\).*\\(}\\)$" (1 'font-lock-reference-face) (2
'font-lock-reference-face))
    ("\\(\\?\\)" 1 'font-lock-reference-face)    
    ("\\(\\,\\)" 1 'font-lock-keyword-face)    
    ("\\(-?[0-9]+?.?[0-9]+\\)" 1 'font-lock-constant-face)    
    ("\\(\\@\\)" 1 'font-lock-preprocessor-face)    
    )
  (list "\.arff?")
  (list
   (function
    (lambda () 
      (setq font-lock-defaults (list 'generic-font-lock-defaults nil t ; case
insensitive
                                     (list (cons ?* "w") (cons ?- "w"))))
      (turn-on-font-lock))))
  "Mode for arff-files.")

# Generates PS, PDF and HTML
#
# FracPete, 2005-10-04

NAME=guide

all: ps pdf html

ps:
	@echo "Generating $@..."
	latex $(NAME).tex
	latex $(NAME).tex
	dvips -o $(NAME).ps $(NAME).dvi

pdf:	ps
	@echo "Generating $@..."
	ps2pdf $(NAME).ps $(NAME).pdf

html:
	@echo "Generating $@..."
	latex2html $(NAME)

clean:
	@echo "Cleaning up..."
	rm -f $(NAME).aux
	rm -f $(NAME).bbl
	rm -f $(NAME).blg
	rm -f $(NAME).dvi
	rm -f $(NAME).log
	rm -f $(NAME).toc
	rm -f $(NAME).ps
	rm -f $(NAME).pdf
	rm -fR $(NAME)
	rm -f *~

all: clean createpdf viewpdf

createpdf: article.tex abstract.tex text.tex bibliography.bib
	latex article
	bibtex article
	latex article
	latex article
	dvips article
	ps2pdf article.ps article.pdf

viewpdf:
	evince article.pdf

clean:
	rm -rf *.log *.pdf *.bbl *.blg *.aux *.ps *.dvi

backup: article.tex abstract.tex text.tex bibliography.bib
	cp article.tex abstract.tex text.tex bibliography.bib backup/
	cp *.eps backup/
	if test -f article.pdf; then cp article.pdf backup/; fi

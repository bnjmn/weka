# Weka Wiki

Not really a wiki, but replacing some of the content that was hosted on
weka.wikispaces.com before it shut down. Uses [mkdocs](http://www.mkdocs.org/) 
to generate the content.


## Installation

*mkdocs* works with Python 2.7 and 3.x.

Best approach is to install mkdocs (>= 0.16.0) in a virtual environment 
(`venv` directory):

* Python 2.7

  ```
  virtualenv -p /usr/bin/python2.7 venv
  ```

* Python 3.6 (Python 3.5 works as well)

  ```
  virtualenv -p /usr/bin/python3.6 venv
  ```

* Install the mkdocs package

  ```
  ./venv/bin/pip install mkdocs
  ```


## Content

In order for content to show up, it needs to be added to the configuration, 
i.e., in the `pages` section of the `mkdocs.yml` file.

Some pointers:

* [Images and media](http://www.mkdocs.org/user-guide/writing-your-docs/#images-and-media)
* [Linking](http://www.mkdocs.org/user-guide/writing-your-docs/#linking-documents)
* [mkdocs.yml](http://www.mkdocs.org/user-guide/configuration/)


## Build

[mkdocs](http://www.mkdocs.org/) is used to generate HTML from the 
markdown documents and images:

```
./venv/bin/mkdocs build --clean
```


## Testing

You can test what the site looks like, using the following command
and opening a browser on [localhost:8000](http://127.0.0.1:8000):

mkdocs monitors setup and markdown files, so you can just add and edit
them as you like, it will automatically rebuild and refresh the browser.

```
./venv/bin/mkdocs build --clean && mkdocs serve
```


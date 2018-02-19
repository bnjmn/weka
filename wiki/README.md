# Weka Wiki

Not really a wiki, but replacing some of the content that was hosted on
weka.wikispaces.com before it shut down.

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
mkdocs build --clean
```

## Testing

You can test what the site looks like, using the following command
and opening a browser on [localhost:8000](http://127.0.0.1:8000):

```
mkdocs build --clean && mkdocs serve
```


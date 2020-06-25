#!/usr/bin/env python3
from flask import Flask, abort, jsonify, redirect, render_template, request
from sqlalchemy import create_engine, MetaData, Table
from urllib.parse import quote, urlunparse
import os

app = Flask(__name__)

connection_string = urlunparse(('postgresql', '%s:%s@%s' % (quote(os.environ['DB_USER']), quote(os.environ['DB_PASSWORD']), os.environ['DB_HOST']), '/' + os.environ['DB_DB'], None, None, None))
engine = create_engine(connection_string, convert_unicode=True)
metadata = MetaData(bind=engine)
labels = Table('labels', metadata, autoload=True)

@app.route('/')
def index():
    return redirect('https://github.com/dice-group/lodcat/tree/testing/lodcat.api')

@app.route('/uri/<type>s', methods=['POST'])
def details(type):
    details = {}
    if (type != 'detail'):
        query = labels.select((labels.c.uri.in_(request.json['uris'])) & (labels.c.type == type))
    else:
        query = labels.select(labels.c.uri.in_(request.json['uris']))
    for row in query.execute():
        uri = row['uri']
        type = row['type']
        value = row['value']
        if uri not in details: details[uri] = {}
        type_pl = type + 's'
        if type_pl not in details[uri]: details[uri][type_pl] = []
        details[uri][type_pl].append(value)
    if request.accept_mimetypes['text/html']:
        return render_template('details.html', details=details)
    if request.accept_mimetypes['application/json'] or request.accept_mimetypes['application/json; charset=UTF-8']:
        return jsonify(details), '200 OK'
    abort(406)

app.run(host='0.0.0.0', port=80)

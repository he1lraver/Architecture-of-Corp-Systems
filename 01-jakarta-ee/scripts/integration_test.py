"""Independent HTTP acceptance checks. Run after START.cmd; Python 3 standard library only."""
import json
import time
import uuid
import urllib.request
import urllib.error
import urllib.parse
import xml.etree.ElementTree as ET
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONFIG = json.loads((ROOT / 'practice.json').read_text('utf-8'))
STAGE = CONFIG['number']
ORIGIN = f"http://127.0.0.1:{CONFIG['httpPort']}"
BASE = ORIGIN + ('' if STAGE == 2 else '/library')
UI = '/' if STAGE == 2 else '/ui'
RESULTS = []

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args): return None

OPENER = urllib.request.build_opener(NoRedirect)

def request(path, method='GET', data=None, xml=False, accept='application/json', form=False):
    if form:
        body = urllib.parse.urlencode(data).encode('utf-8')
        content_type = 'application/x-www-form-urlencoded'
    else:
        body = (data if xml else json.dumps(data)).encode('utf-8') if data is not None else None
        content_type = 'application/xml' if xml else 'application/json'
    req = urllib.request.Request((path if path.startswith('http') else BASE + path), data=body, method=method,
          headers={'Content-Type':content_type, 'Accept':accept})
    try: response = OPENER.open(req, timeout=15)
    except urllib.error.HTTPError as error: response = error
    with response: return response.status, response.read().decode('utf-8'), response.headers

def check(name, condition):
    RESULTS.append({'test':name, 'passed':bool(condition)})
    print(('PASS ' if condition else 'FAIL ') + name)
    if not condition: raise AssertionError(name)

def eventually(condition, timeout=30):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        if condition(): return True
        time.sleep(.3)
    return False

class Forms(HTMLParser):
    def __init__(self, html):
        super().__init__(); self.rows=[]; self.row=None; self.feed(html)
    def handle_starttag(self, tag, attrs):
        attrs=dict(attrs)
        if tag=='form': self.row={'action':attrs['action']}
        elif tag=='input' and self.row is not None and 'name' in attrs:
            self.row[attrs['name']]=attrs.get('value','')
    def handle_endtag(self, tag):
        if tag=='form': self.rows.append(self.row); self.row=None

class AuditRows(HTMLParser):
    def __init__(self, html):
        super().__init__(); self.rows=[]; self.row=None; self.cell=None; self.feed(html)
    def handle_starttag(self, tag, attrs):
        if tag=='article': self.row=[]
        if tag=='pre' and self.row is not None: self.cell=''
    def handle_data(self, data):
        if self.cell is not None: self.cell+=data
    def handle_endtag(self, tag):
        if tag=='pre' and self.cell is not None: self.row.append(self.cell); self.cell=None
        if tag=='article' and self.row is not None: self.rows.append(self.row); self.row=None

def forms(): return Forms(request(UI,accept='text/html')[1]).rows
def submit(path, data): return request(path,'POST',data,form=True,accept='text/html')[0]

def form_crud():
    suffix=uuid.uuid4().hex[:10]
    author=book=None
    try:
        check('HTML catalog responds',request(UI,accept='text/html')[0]==200)
        a={'name':'HTTP-author-'+suffix,'country':'RU','birthYear':1980}
        check('Forms create author',submit('/authors',a) in (302,303))
        author=next(row['id'] for row in forms() if row.get('name')==a['name'])
        a.update(id=author,country='Updated')
        check('Forms update author',submit('/authors',a) in (302,303) and next(row for row in forms() if row.get('id')==author and 'name' in row)['country']=='Updated')
        b={'title':'HTTP-book-'+suffix,'publishedYear':2020,'price':'99.50','stock':5,'authorId':author}
        check('Forms create book',submit('/books',b) in (302,303))
        book=next(row['id'] for row in forms() if row.get('title')==b['title'])
        check('Forms forbid deleting author with books',submit('/authors',{'id':author,'operation':'delete'})==409)
        b.update(id=book,price='123.45')
        check('Forms update book and persist price',submit('/books',b) in (302,303) and next(row for row in forms() if row.get('id')==book and 'title' in row)['price']=='123.45')
        check('Forms reject negative stock',submit('/books',{**b,'stock':-1})==400)
        check('Forms failed edit preserves stock',next(row for row in forms() if row.get('id')==book and 'title' in row)['stock']=='5')
        check('Forms delete book',submit('/books',{'id':book,'operation':'delete'}) in (302,303))
        book=None
        check('Forms delete author',submit('/authors',{'id':author,'operation':'delete'}) in (302,303))
        author=None
        check('Forms deletions are persisted',not any(suffix in str(row) for row in forms()))
        check('Forms reject blank author',submit('/authors',{'name':'','country':'RU','birthYear':1980})==400)
    finally:
        if book is not None: submit('/books',{'id':book,'operation':'delete'})
        if author is not None: submit('/authors',{'id':author,'operation':'delete'})
    if STAGE < 3:
        check('Early practice has no REST endpoint',request('/api/books')[0]==404)
    if STAGE != 4:
        check('Earlier practice has no audit endpoint',request('/audit')[0]==404)

def rest_crud():
    suffix=uuid.uuid4().hex[:10]
    author=book=None
    try:
        status,body,_=request('/api/authors')
        check('JSON authors list',status==200 and isinstance(json.loads(body)['items'],list))
        check('Scalar JSON returns 400',request('/api/authors','POST','not an object')[0]==400)
        check('Empty JSON object returns 400',request('/api/authors','POST',{})[0]==400)
        check('Incomplete XML returns 400',request('/api/authors','POST','<author/>',True)[0]==400)
        check('XML DTD rejected',request('/api/authors','POST','<!DOCTYPE author [<!ENTITY x "entity">]><author><name>&x;</name><country>RU</country><birthYear>1980</birthYear></author>',True)[0]==400)
        check('Oversized REST body returns 413',request('/api/authors','POST',{'name':'x'*66000})[0]==413)
        status,body,headers=request('/api/authors','POST',{'name':'API-'+suffix,'country':'RU','birthYear':1980})
        check('JSON create author with Location',status==201 and 'Location' in headers)
        author=json.loads(body)['id']
        status,body,_=request(f'/api/authors/{author}',accept='application/xml')
        check('XML author with stylesheet',status==200 and '<?xml-stylesheet' in body and ET.fromstring(body).findtext('name')=='API-'+suffix)
        status,body,_=request(f'/api/authors/{author}','PUT',f'<author><name>Updated-{suffix}</name><country>RU</country><birthYear>1981</birthYear></author>',True)
        check('XML input and JSON output',status==200 and json.loads(body)['birthYear']==1981)
        status,body,_=request('/api/books','POST',f'<book><title>API-book-{suffix}</title><publishedYear>2020</publishedYear><price>99.50</price><stock>1</stock><authorId>{author}</authorId></book>',True,'application/xml')
        check('XML create book and XML response',status==201 and '<?xml-stylesheet' in body)
        book=int(ET.fromstring(body).findtext('id'))
        check('REST relation conflict returns 409',request(f'/api/authors/{author}','DELETE')[0]==409)
        payload={'title':'Updated-book-'+suffix,'publishedYear':2021,'price':101.25,'stock':2,'authorId':author}
        status,body,_=request(f'/api/books/{book}','PUT',payload,accept='application/xml')
        check('JSON input and XML output',status==200 and ET.fromstring(body).findtext('stock')=='2')
        check('Negative REST stock returns 400',request(f'/api/books/{book}','PUT',{**payload,'stock':-1})[0]==400)
        check('Failed REST update preserves data',json.loads(request(f'/api/books/{book}')[1])['stock']==2)
        for resource in ('authors','books'):
            status,body,_=request(f'/api/{resource}?format=xml')
            check(f'XML {resource} list and stylesheet',status==200 and '<?xml-stylesheet' in body and ET.fromstring(body).tag==resource)
        check('Static XSL available',request('/library.xsl')[0]==200 and 'authorId' in request('/library.xsl')[1])
        status,html,_=request('/view/books',accept='text/html')
        check('XSL HTML preview and book-to-author navigation',status==200 and 'XML' in html and f'/library/view/authors/{author}' in html)
        check('XSL author detail preview',request(f'/view/authors/{author}',accept='text/html')[0]==200)
        check('XSL book detail preview',request(f'/view/books/{book}',accept='text/html')[0]==200)
        check('REST deletes book without body',request(f'/api/books/{book}','DELETE')[:2]==(204,''))
        deleted_book=book; book=None
        status,body,_=request(f'/api/books/{deleted_book}',accept='application/xml')
        check('XML 404 also has stylesheet',status==404 and '<?xml-stylesheet' in body and ET.fromstring(body).tag=='error')
        check('REST deletes author',request(f'/api/authors/{author}','DELETE')[0]==204)
        deleted_author=author; author=None
        if STAGE==4: check_jms(deleted_author,deleted_book)
    finally:
        if book is not None: request(f'/api/books/{book}','DELETE')
        if author is not None: request(f'/api/authors/{author}','DELETE')

def check_jms(author,book):
    def rows():
        return [r for r in AuditRows(request('/audit')[1]).rows if len(r)==6 and (r[3],r[4]) in {('Author',str(author)),('Book',str(book))}]
    def complete():
        data=rows()
        return len(data)==6 and len({r[0] for r in data})==6 and {(r[3],r[2]) for r in data}=={(e,op) for e in ('Author','Book') for op in ('INSERT','UPDATE','DELETE')}
    check('JMS six unique changes; rejected edit creates no event',eventually(complete))
    for row in rows():
        snapshots=json.loads(row[5])
        check(f'JMS {row[3]} {row[2]} snapshot contract',
              (snapshots['before'] is None)==(row[2]=='INSERT') and (snapshots['after'] is None)==(row[2]=='DELETE'))
    def messages():
        status,body,_=request(f"http://127.0.0.1:{CONFIG['mailPort']}/api/v1/messages?limit=200")
        return json.loads(body)['messages']
    def matching(): return [m for m in messages() if m['Subject']==f'Low book stock #{book}']
    check('Independent JMS subscriber sends two low-stock messages',eventually(lambda:len(matching())==2))
    for message in matching():
        check('Mail sent to configured local recipient',any(r['Address']=='librarian@library.test' for r in message['To']))
        status,body,_=request(f"http://127.0.0.1:{CONFIG['mailPort']}/api/v1/message/{message['ID']}")
        detail=json.loads(body)
        check('Mail includes audited event identifier',any(r[0] in detail['Text'] for r in rows() if r[3]=='Book'))
    # The form scenario uses stock=5. After the two REST mails, no other event qualifies.
    time.sleep(1)
    check('No notification for author, delete or ordinary stock',len(messages())==INITIAL_MAIL_COUNT+2)

INITIAL_MAIL_COUNT=0
if __name__=='__main__':
    try:
        if STAGE==4:
            _,body,_=request(f"http://127.0.0.1:{CONFIG['mailPort']}/api/v1/messages?limit=200")
            INITIAL_MAIL_COUNT=json.loads(body)['total']
        form_crud()
        if STAGE>=3: rest_crud()
    finally:
        (ROOT/'evidence').mkdir(exist_ok=True)
        (ROOT/'evidence/integration-results.json').write_text(json.dumps(RESULTS,ensure_ascii=False,indent=2),encoding='utf-8')

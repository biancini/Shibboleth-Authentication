import os
import sys

path = "/opt/"
if path not in sys.path:
    sys.path.append(path)

path = "/opt/access_web"
if path not in sys.path:
    sys.path.append(path)

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "access_web.settings")

import django.core.handlers.wsgi
application = django.core.handlers.wsgi.WSGIHandler()

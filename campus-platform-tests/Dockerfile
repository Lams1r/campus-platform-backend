FROM python:3.12-slim

WORKDIR /tests

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY campus_tests ./campus_tests
COPY tests ./tests
COPY pytest.ini .

RUN mkdir -p reports

CMD ["python", "-m", "pytest", "tests", "-p", "campus_tests.pytest_dashboard", "--html=reports/campus-test-report.html", "--self-contained-html"]

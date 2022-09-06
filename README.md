# AWScanner

AWScanner is a tool for pulling information about resources in AWS accounts.
It's an experiment (i.e., toy) right now, so I'm not sure where it's going but
my current thought is it will focus on billing and minor security issues.

**This is experimental-quality code. Do not rely on it for production usage.
The API will change, it's probably unstable, and I'm sure there are bugs. It 
will almost certainly kick your dog and burn down your house.** (Feedback and
issue reports are welcome though.)


## Usage

Currently, it is expected that session credentials are stored in 
`~/.aws/credentials` (see [here](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html) 
for reference). 

```bash
python3 -m venv ./venv && source venv/bin/activate
pip install -r requirements.txt
python3 awscanner/main.py --profile MY_ACCOUNT_PROFILE_NAME
```

Usage help is available via `--help`.

## Security

Please report security issues to security -at- robeden -dot- com.
from dataclasses import dataclass
from typing import Dict, List, Optional, Iterator, TypeVar, Callable

import boto3 as boto3
from botocore.exceptions import ClientError

from awscraper.util import IdentifiedResource, parse_tags


@dataclass
class BucketInfo(IdentifiedResource):
    acl_grants: Optional[List[Dict]]
    website_configured: bool
    policy: str
    policy_public: bool
    block_public_acls: bool
    ignore_public_acls: bool
    block_public_policy: bool
    restrict_public_buckets: bool


R = TypeVar('R')
def client_error_handler(query_function: Callable[[], R],
                         accepted_error: str,
                         accepted_error_value: Optional[R] = None) -> Optional[R]:
    try:
        return query_function()
    except ClientError as e:
        if e.response['Error']['Code'] == accepted_error:
            return accepted_error_value
        raise


def gather(session: boto3.Session) -> Iterator[BucketInfo]:
    client = session.client("s3")

    for bucket in client.list_buckets()["Buckets"]:
        location = client.get_bucket_location(Bucket=bucket["Name"])["LocationConstraint"]
        if location is None:
            location = "us-east-1"
        if location != session.region_name:
            continue

        def __get_website():
            client.get_bucket_website(Bucket=bucket['Name'])
            return True

        website_configured = client_error_handler(
            __get_website,
            "NoSuchWebsiteConfiguration",
            False)

        tags = client_error_handler(
            lambda: parse_tags(client.get_bucket_tagging(Bucket=bucket["Name"])["TagSet"]),
            'NoSuchTagSet',
            {})

        policy = client_error_handler(
            lambda: client.get_bucket_policy(Bucket=bucket["Name"])["Policy"],
            "NoSuchBucketPolicy")

        policy_status = client_error_handler(
            lambda: client.get_bucket_policy_status(Bucket=bucket["Name"])["PolicyStatus"]["IsPublic"],
            "NoSuchBucketPolicy")

        pabc = client_error_handler(
            lambda: client.get_public_access_block(Bucket=bucket["Name"])["PublicAccessBlockConfiguration"],
            "NoSuchPublicAccessBlockConfiguration",
            {})

        yield BucketInfo(
            bucket["Name"],
            tags,
            client.get_bucket_acl(Bucket=bucket["Name"])["Grants"],
            website_configured,
            policy,
            policy_status,
            pabc.get('BlockPublicAcls', False),
            pabc.get('IgnorePublicAcls', False),
            pabc.get('BlockPublicPolicy', False),
            pabc.get('RestrictPublicBuckets', False)
        )

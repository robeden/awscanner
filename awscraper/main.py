import boto3


ec2_client = boto3.client("ec2")
sts_client = boto3.client("sts")

if __name__ == '__main__':
    caller = sts_client.get_caller_identity()
    print(f"Caller: user={caller.get('UserId')} account={caller.get('Account')}")

    regions = [r.get("RegionName") for r in ec2_client.describe_regions(AllRegions=False).get("Regions")]
    print(f"Regions: {regions}")

    is_gov = regions[0].startswith("us-gov")

    